package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

/**
 * Indexed in-memory representation of a resource pack's virtual files, providing
 * O(1) path lookups, prefix queries, duplicate grouping, and deterministic sorted output.
 */
public final class PackAssetIndex {

    private final Map<String, List<VirtualFile>> filesByPath = new LinkedHashMap<>();

    public PackAssetIndex() {
    }

    public PackAssetIndex(@NotNull Collection<VirtualFile> initialFiles) {
        addAll(initialFiles);
    }

    public static @NotNull String normalizePath(@NotNull String rawPath) {
        String p = rawPath.replace('\\', '/').trim();
        while (p.contains("//")) {
            p = p.replace("//", "/");
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    public void add(@NotNull VirtualFile file) {
        String key = normalizePath(file.getPath());
        file.setPath(key);
        filesByPath.computeIfAbsent(key, k -> new ArrayList<>()).add(file);
    }

    public void addAll(@NotNull Collection<VirtualFile> files) {
        for (VirtualFile file : files) {
            if (file != null) {
                add(file);
            }
        }
    }

    public void putSingle(@NotNull VirtualFile file) {
        String key = normalizePath(file.getPath());
        file.setPath(key);
        List<VirtualFile> list = new ArrayList<>(1);
        list.add(file);
        filesByPath.put(key, list);
    }

    public void putMerged(@NotNull VirtualFile file, @NotNull BinaryOperator<VirtualFile> merger) {
        String key = normalizePath(file.getPath());
        file.setPath(key);
        List<VirtualFile> existing = filesByPath.get(key);
        if (existing == null || existing.isEmpty()) {
            putSingle(file);
            return;
        }
        VirtualFile merged = existing.get(0);
        for (int i = 1; i < existing.size(); i++) {
            merged = merger.apply(merged, existing.get(i));
        }
        merged = merger.apply(merged, file);
        putSingle(merged);
    }

    public @Nullable VirtualFile getFirst(@NotNull String path) {
        List<VirtualFile> list = filesByPath.get(normalizePath(path));
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    public @NotNull List<VirtualFile> getAll(@NotNull String path) {
        List<VirtualFile> list = filesByPath.get(normalizePath(path));
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public boolean contains(@NotNull String path) {
        List<VirtualFile> list = filesByPath.get(normalizePath(path));
        return list != null && !list.isEmpty();
    }

    public boolean remove(@NotNull String path) {
        return filesByPath.remove(normalizePath(path)) != null;
    }

    public void removeIf(@NotNull Predicate<VirtualFile> filter) {
        Iterator<Map.Entry<String, List<VirtualFile>>> it = filesByPath.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<VirtualFile>> entry = it.next();
            entry.getValue().removeIf(filter);
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    public @NotNull List<VirtualFile> findByPrefix(@NotNull String prefix) {
        String normalizedPrefix = normalizePath(prefix);
        List<VirtualFile> result = new ArrayList<>();
        for (Map.Entry<String, List<VirtualFile>> entry : filesByPath.entrySet()) {
            if (entry.getKey().startsWith(normalizedPrefix)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    public @NotNull Map<String, List<VirtualFile>> findDuplicates(@NotNull Predicate<String> pathFilter) {
        Map<String, List<VirtualFile>> duplicates = new LinkedHashMap<>();
        for (Map.Entry<String, List<VirtualFile>> entry : filesByPath.entrySet()) {
            if (entry.getValue().size() > 1 && pathFilter.test(entry.getKey())) {
                duplicates.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        return duplicates;
    }

    public @NotNull List<VirtualFile> toFlatList() {
        List<VirtualFile> result = new ArrayList<>();
        for (List<VirtualFile> bucket : filesByPath.values()) {
            result.addAll(bucket);
        }
        return result;
    }

    public @NotNull List<VirtualFile> toSortedList() {
        List<VirtualFile> list = toFlatList();
        Collections.sort(list);
        return list;
    }
}
