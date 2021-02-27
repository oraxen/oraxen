package org.playuniverse.snowypine.command.argument;

import java.util.function.Function;

import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.IntegerArgument;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;

public final class CompletionHelper {

	private CompletionHelper() {}

	/*
	 * 
	 * Primitive helper
	 * 
	 */

	public static void complete(DefaultCompletion completion, String... strings) {
		for (String string : strings) {
			completion.addSafe(new StringArgument(string));
		}
	}

	public static void complete(DefaultCompletion completion, Integer... integers) {
		for (Integer integer : integers) {
			completion.addSafe(new IntegerArgument(integer));
		}
	}

	@SafeVarargs
	public static <E> void complete(DefaultCompletion completion, Function<E, String> mapper, E... values) {
		for (E value : values) {
			completion.addSafe(new StringArgument(mapper.apply(value)));
		}
	}

	@SafeVarargs
	public static <E> void completeMulti(DefaultCompletion completion, Function<E, String[]> mapper, E... values) {
		for (E value : values) {
			complete(completion, mapper.apply(value));
		}
	}

}
