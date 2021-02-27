package org.playuniverse.snowypine.config;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.playuniverse.snowypine.language.Message;
import org.playuniverse.snowypine.utils.general.Placeholder;

public final class ConfigTimer implements Runnable {

	public static final ConfigTimer TIMER = new ConfigTimer();

	private final ArrayList<Config> reload = new ArrayList<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final ReadLock read = lock.readLock();
	private final WriteLock write = lock.writeLock();

	private ConfigTimer() {

	}

	public boolean load(Config config) {
		boolean output;
		read.lock();
		if (output = !reload.contains(config)) {
			read.unlock();
			write.lock();
			try {
				reload.add(config);
			} finally {
				write.unlock();
			}
		}
		return output;
	}

	public boolean unload(Config config) {
		boolean output;
		read.lock();
		if (output = reload.contains(config)) {
			read.unlock();
			write.lock();
			try {
				reload.remove(config);
			} finally {
				write.unlock();
			}
		}
		return output;
	}

	@Override
	public void run() {
		read.lock();
		try {
			for (Config config : reload) {
				if (config.loaded < config.file.lastModified()) {
					Message.CONFIG_RELOAD_NEEDED.sendConsole(Placeholder.of("name", config.getName()));
					config.reload();
					Message.CONFIG_RELOAD_DONE.sendConsole(Placeholder.of("name", config.getName()));
				}
			}
		} finally {
			read.unlock();
		}
	}

}
