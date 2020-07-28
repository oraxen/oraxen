package io.th0rgal.oraxen.language;

public enum DescriptionType {
	
	SIMPLE(true), DETAILED();
	
	private final boolean simple;
	
	private DescriptionType() {
		simple = false;
	}
	
	private DescriptionType(boolean simple) {
		this.simple = simple;
	}
	
	/*
	 * 
	 */
	
	public boolean isSimple() {
		return simple;
	}

}
