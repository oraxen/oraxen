package org.playuniverse.snowypine.bukkit.inventory;

public enum MoveInventory {

	SOURCE_INITIATOR(true, true),
	SOURCE_DESTINATION(false, true),
	DESTINATION(false, false),
	INITIATOR(true, false);

	private final boolean initiator, source;

	private MoveInventory(boolean initiator, boolean source) {
		this.initiator = initiator;
		this.source = source;
	}

	public boolean isInitiator() {
		return initiator;
	}

	public boolean isSource() {
		return source;
	}

	public static MoveInventory of(boolean initiator, boolean source) {
		if (initiator) {
			if (source) {
				return MoveInventory.SOURCE_INITIATOR;
			}
			return MoveInventory.INITIATOR;
		}
		if (source) {
			return MoveInventory.SOURCE_DESTINATION;
		}
		return MoveInventory.DESTINATION;
	}

}
