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
        for(int index = 0; index < strings.length; index++)
            completion.add(new StringArgument(strings[index]));
    }
    
    public static void completion(DefaultCompletion completion, Integer... ints) {
        for(int index = 0; index < ints.length; index++)
            completion.add(new IntegerArgument(ints[index]));
    }

}
