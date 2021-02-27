package org.playuniverse.snowypine.command.permission;

public interface ISnowyPermission extends IPermission {
	
	@Override
	default String prefix() {
		return "snowypine";
	}
	
	String module();

}
