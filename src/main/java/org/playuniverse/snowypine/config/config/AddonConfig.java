package org.playuniverse.snowypine.config.config;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.compatibility.CompatibilityHandler;
import org.playuniverse.snowypine.config.Config;
import org.playuniverse.snowypine.config.migration.AddonMigration;
import org.playuniverse.snowypine.language.IVariable;
import org.playuniverse.snowypine.language.Variable;

public final class AddonConfig extends Config {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ArrayList<String> disabled = new ArrayList<>();

	private final ReadLock read = lock.readLock();
	private final WriteLock write = lock.writeLock();

	private boolean skip = true;

	protected AddonConfig() {
		super(new File(Snowypine.getPlugin().getDirectory(), "modules.yml"), AddonMigration.class, 1);
	}

	/*
	 * Type
	 */

	@Override
	protected IVariable getName() {
		return Variable.CONFIG_TYPE_MODULE;
	}

	/*
	 * Handle
	 */

	@Override
	protected void onSetup() {
		load(false);
	}

	@Override
	protected void onLoad() {
		if (skip) {
			skip = false;
			return;
		}
		load(true);
	}

	@Override
	protected void onUnload() {

	}

	private void load(boolean refresh) {
		write.lock();
		try {
			disabled.clear();
			for (String compat : CompatibilityHandler.getCompatibilityNames()) {
				if (check(compat)) {
					disabled.add(compat);
				}
			}
			read.lock();
		} finally {
			write.unlock();
		}
		try {
			if (refresh) {
				CompatibilityHandler.handleSettingsUpdate(Snowypine.SETTINGS);
			}
		} finally {
			read.unlock();
		}
	}

	/*
	 * Method
	 */

	private boolean check(String compat) {
		return !check("addons." + compat, true);
	}

	public boolean isDisabled(String name) {
		read.lock();
		try {
			return this.disabled.contains(name);
		} finally {
			read.unlock();
		}
	}

}
