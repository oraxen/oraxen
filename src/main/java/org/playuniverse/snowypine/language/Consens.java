package org.playuniverse.snowypine.language;

import org.playuniverse.snowypine.utils.general.Placeholder;

import com.syntaxphoenix.syntaxapi.utils.java.UniCode;
import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;

public final class Consens {

	private Consens() {}

	private static final Container<Placeholder[]> PLACEHOLDER = Container.of();

	public static Placeholder[] get() {
		if (PLACEHOLDER.isPresent()) {
			return PLACEHOLDER.get();
		}
		return PLACEHOLDER.replace(new Placeholder[] {
				Placeholder.of(false, "%ae", UniCode.LOWER_AE),
				Placeholder.of(false, "%AE", UniCode.UPPER_AE),
				Placeholder.of(false, "%oe", UniCode.LOWER_OE),
				Placeholder.of(false, "%OE", UniCode.UPPER_OE),
				Placeholder.of(false, "%ue", UniCode.LOWER_UE),
				Placeholder.of(false, "%UE", UniCode.UPPER_UE),
				Placeholder.of(false, "%>>", UniCode.ARROWS_RIGHT),
				Placeholder.of(false, "%<<", UniCode.ARROWS_LEFT),
		}).get();
	}

}
