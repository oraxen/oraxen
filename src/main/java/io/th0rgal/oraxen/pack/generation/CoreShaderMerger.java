package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Conservative composition for core shaders with compatible global declarations. */
final class CoreShaderMerger {
    private static final int MAX_SOURCE_BYTES = 262_144;
    private static final long MAX_STATEMENT_PAIRS = 250_000;
    private static final Pattern MAIN = Pattern.compile("void\\s+main\\s*\\(\\s*(?:void\\s*)?\\)\\s*\\{");
    private static final Pattern FUNCTION = Pattern.compile("(?:\\w+\\s+)+(\\w+)\\s*\\([^;{}]*\\)\\s*\\{");
    private static final Pattern VARIABLE = Pattern.compile("(?:\\w+\\s+)+(\\w+)\\s*(?:\\[\\s*\\d*\\s*])?\\s*(?:=[^;]+)?;");

    private CoreShaderMerger() {}

    static boolean isCoreShader(String path) {
        return path.matches("assets/minecraft/shaders/core/.+\\.(?:vsh|fsh|json)");
    }

    static byte[] merge(String path, byte[] lower, byte[] higher) {
        if (Arrays.equals(lower, higher)) return higher;
        if (lower.length > MAX_SOURCE_BYTES || higher.length > MAX_SOURCE_BYTES)
            throw new IllegalArgumentException("Shader exceeds merge size limit");
        String low = new String(lower, StandardCharsets.UTF_8);
        String high = new String(higher, StandardCharsets.UTF_8);
        return (path.endsWith(".json") ? mergeProgram(low, high) : mergeSource(low, high))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String mergeProgram(String low, String high) {
        JsonObject result = JsonParser.parseString(low).getAsJsonObject().deepCopy();
        JsonObject incoming = JsonParser.parseString(high).getAsJsonObject();
        for (var entry : incoming.entrySet()) {
            JsonElement previous = result.get(entry.getKey());
            if (previous == null) result.add(entry.getKey(), entry.getValue());
            else if (!previous.equals(entry.getValue())) {
                if (!Set.of("attributes", "samplers", "uniforms").contains(entry.getKey()))
                    throw new IllegalArgumentException("Conflicting shader program property " + entry.getKey());
                JsonArray combined = previous.getAsJsonArray();
                for (JsonElement value : entry.getValue().getAsJsonArray()) {
                    String name = programEntryName(value);
                    JsonElement existing = null;
                    for (JsonElement candidate : combined)
                        if (programEntryName(candidate).equals(name)) existing = candidate;
                    if (existing == null) combined.add(value);
                    else if (!existing.equals(value))
                        throw new IllegalArgumentException("Conflicting shader program entry " + name);
                }
            }
        }
        return result.toString();
    }

    private static String programEntryName(JsonElement value) {
        return value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("name").getAsString();
    }

    private static String mergeSource(String low, String high) {
        Source first = parse(low);
        Source second = parse(high);
        if (!first.version.equals(second.version)) throw new IllegalArgumentException("Different GLSL versions");
        LinkedHashMap<String, String> declarations = new LinkedHashMap<>(first.declarations);
        second.declarations.forEach((name, declaration) -> {
            String previous = declarations.putIfAbsent(name, declaration);
            if (previous != null && !normalize(previous).equals(normalize(declaration)))
                throw new IllegalArgumentException("Conflicting GLSL declaration " + name);
        });
        Set<String> imports = new LinkedHashSet<>(first.imports);
        imports.addAll(second.imports);
        List<String> lowStatements = statements(first.main);
        List<String> highStatements = statements(second.main);
        if (Pattern.compile("\\b(?:return|do)\\b").matcher(first.main + second.main).find())
            throw new IllegalArgumentException("Early returns and do-while loops cannot be composed safely");
        if ((long) lowStatements.size() * highStatements.size() > MAX_STATEMENT_PAIRS)
            throw new IllegalArgumentException("Shader exceeds merge work limit");
        List<String> lowOrder = lowStatements.stream().map(CoreShaderMerger::normalize).toList();
        List<String> highOrder = highStatements.stream().map(CoreShaderMerger::normalize).toList();
        Set<String> shared = new HashSet<>(lowOrder);
        shared.retainAll(highOrder);
        if (!lowOrder.stream().filter(shared::contains).toList().equals(highOrder.stream().filter(shared::contains).toList()))
            throw new IllegalArgumentException("Conflicting shared statement order");
        validateLocals(first.main, second.main, declarations.keySet());
        // Keep shared vanilla statements once and retain each pack's additions in order.
        int[][] common = new int[lowStatements.size() + 1][highStatements.size() + 1];
        for (int i = lowStatements.size() - 1; i >= 0; i--)
            for (int j = highStatements.size() - 1; j >= 0; j--)
                common[i][j] = normalize(lowStatements.get(i)).equals(normalize(highStatements.get(j)))
                        ? 1 + common[i + 1][j + 1] : Math.max(common[i + 1][j], common[i][j + 1]);
        List<String> merged = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < lowStatements.size() || j < highStatements.size()) {
            if (i == lowStatements.size()) merged.add(highStatements.get(j++));
            else if (j == highStatements.size()) merged.add(lowStatements.get(i++));
            else if (normalize(lowStatements.get(i)).equals(normalize(highStatements.get(j)))) {
                merged.add(highStatements.get(j++));
                i++;
            } else if (common[i + 1][j] >= common[i][j + 1]) merged.add(lowStatements.get(i++));
            else merged.add(highStatements.get(j++));
        }
        return first.version + "\n" + String.join("\n", imports) + "\n"
                + String.join("\n", declarations.values()) + "\nvoid main() {\n"
                + String.join("\n", merged) + "\n}\n";
    }

    private static void validateLocals(String low, String high, Set<String> globals) {
        Map<String, String> lowLocals = locals(low);
        Map<String, String> highLocals = locals(high);
        for (var side : List.of(lowLocals, highLocals)) {
            String otherSource = side == lowLocals ? high : low;
            Map<String, String> otherLocals = side == lowLocals ? highLocals : lowLocals;
            for (var local : side.entrySet()) {
                if (globals.contains(local.getKey()))
                    throw new IllegalArgumentException("Local shadows GLSL global " + local.getKey());
                if (Pattern.compile("\\b" + Pattern.quote(local.getKey()) + "\\b").matcher(otherSource).find()
                        && !local.getValue().equals(otherLocals.get(local.getKey())))
                    throw new IllegalArgumentException("Conflicting GLSL local " + local.getKey());
            }
        }
    }

    private static Map<String, String> locals(String main) {
        Map<String, String> names = new LinkedHashMap<>();
        Matcher declarations = Pattern.compile("(?<![\\w.])(?:(?:const|highp|mediump|lowp)\\s+)*"
                + "(?:bool|int|uint|float|double|[biud]?vec[234]|d?mat[234](?:x[234])?)\\s+(\\w+)\\s*([^;]*);").matcher(main);
        while (declarations.find()) {
            String tail = declarations.group(2);
            rejectMultipleDeclarators(tail);
            if (names.putIfAbsent(declarations.group(1), normalize(declarations.group())) != null)
                throw new IllegalArgumentException("Repeated GLSL local " + declarations.group(1));
        }
        return names;
    }

    private static void rejectMultipleDeclarators(String declaration) {
        int depth = 0;
        for (char c : declaration.toCharArray()) {
            if (c == '(' || c == '[') depth++;
            if (c == ')' || c == ']') depth--;
            if (c == ',' && depth == 0)
                throw new IllegalArgumentException("Multiple GLSL declarators are unsupported");
        }
    }

    private static List<String> statements(String main) {
        String body = main.substring(main.indexOf('{') + 1, main.lastIndexOf('}'));
        Matcher controls = Pattern.compile("\\b(?:if|for|while)\\s*\\(").matcher(body);
        while (controls.find()) {
            int end = controls.end();
            int nesting = 1;
            while (end < body.length() && nesting > 0) {
                char c = body.charAt(end++);
                if (c == '(') nesting++;
                if (c == ')') nesting--;
            }
            while (end < body.length() && Character.isWhitespace(body.charAt(end))) end++;
            if (nesting != 0 || end == body.length() || body.charAt(end) != '{')
                throw new IllegalArgumentException("Unbraced GLSL control flow is unsupported");
        }
        if (Pattern.compile("\\belse\\s+(?!if\\b|\\s*\\{)\\S").matcher(body).find())
            throw new IllegalArgumentException("Unbraced GLSL else is unsupported");
        List<String> result = new ArrayList<>();
        int depth = 0;
        int parentheses = 0;
        int start = 0;
        for (int index = 0; index < body.length(); index++) {
            char c = body.charAt(index);
            if (c == '{') depth++;
            if (c == '}') depth--;
            if (c == '(') parentheses++;
            if (c == ')') parentheses--;
            if (depth != 0 || parentheses != 0) continue;
            String tail = body.substring(index + 1).stripLeading();
            if (c != ';' && (c != '}' || tail.startsWith("else") || tail.startsWith(";"))) continue;
            result.add(body.substring(start, index + 1).strip());
            start = index + 1;
        }
        if (!body.substring(start).isBlank() || depth != 0 || parentheses != 0)
            throw new IllegalArgumentException("Unsupported GLSL main body");
        return result;
    }

    private static Source parse(String source) {
        String clean = source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\r\\n]*", "");
        String version = null;
        List<String> imports = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        for (String line : clean.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("#version ") && version == null) version = trimmed;
            else if (trimmed.matches("#moj_import\\s+[<\"].+[>\"]")) imports.add(trimmed);
            else if (trimmed.startsWith("#")) throw new IllegalArgumentException("Unsupported GLSL preprocessor directive");
            else body.append(line).append('\n');
        }
        if (version == null) throw new IllegalArgumentException("Missing GLSL version");
        Map<String, String> declarations = new LinkedHashMap<>();
        String main = null;
        int start = 0;
        int depth = 0;
        for (int index = 0; index < body.length(); index++) {
            char c = body.charAt(index);
            if (c == '{') depth++;
            if (c == '}' && --depth < 0) throw new IllegalArgumentException("Unbalanced GLSL braces");
            if (depth != 0 || c != ';' && c != '}') continue;
            String declaration = body.substring(start, index + 1).strip();
            start = index + 1;
            if (MAIN.matcher(declaration).lookingAt()) {
                if (main != null) throw new IllegalArgumentException("Multiple GLSL entry points");
                main = declaration;
            } else {
                Matcher function = FUNCTION.matcher(declaration);
                Matcher variable = VARIABLE.matcher(declaration);
                String name;
                if (function.lookingAt()) name = function.group(1);
                else if (variable.matches()) {
                    rejectMultipleDeclarators(declaration);
                    if (declaration.contains("=") && !declaration.startsWith("const "))
                        throw new IllegalArgumentException("Non-constant GLSL initializer is unsupported");
                    name = variable.group(1);
                }
                else throw new IllegalArgumentException("Unsupported GLSL declaration");
                if (declarations.putIfAbsent(name, declaration) != null)
                    throw new IllegalArgumentException("Repeated GLSL declaration " + name);
            }
        }
        if (depth != 0 || !body.substring(start).isBlank() || main == null)
            throw new IllegalArgumentException("Incomplete GLSL source");
        return new Source(version, imports, declarations, main);
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").strip();
    }

    private record Source(String version, List<String> imports, Map<String, String> declarations, String main) {}
}
