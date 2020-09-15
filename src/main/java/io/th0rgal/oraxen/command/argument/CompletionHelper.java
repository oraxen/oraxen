package io.th0rgal.oraxen.command.argument;

import java.util.function.Function;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.IntegerArgument;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;

public abstract class CompletionHelper {

    /*
     * 
     * Primitive helper
     * 
     */

    public static void completion(DefaultCompletion completion, String... strings) {
        for (String string : strings)
            completion.addSafe(new StringArgument(string));
    }

    public static void completion(DefaultCompletion completion, Integer... integers) {
        for (Integer integer : integers)
            completion.addSafe(new IntegerArgument(integer));
    }

    public static void completionMap(DefaultCompletion completion, Function<String, String> mapper, String... strings) {
        Arguments arguments = completion.getCompletion();
        for (String string : strings) {
            arguments
                .add(new StringArgument(
                    arguments.match((current) -> current.asObject().equals(string)) ? "oraxen:" + string : string));
        }
    }

}
