package io.th0rgal.oraxen.command.argument;

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
        for (String string : strings) completion.add(new StringArgument(string));
    }

    public static void completion(DefaultCompletion completion, Integer... ints) {
        for (Integer anInt : ints) completion.add(new IntegerArgument(anInt));
    }

}
