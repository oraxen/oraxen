package org.playuniverse.snowypine.utils.general;

public class Cooldown {

	private final CooldownTimer timer;

	private long cooldown;
	private long threshhold = 0;

	public Cooldown(long cooldown) {
		this.cooldown = cooldown;
		this.timer = new CooldownTimer(this);
		timer.setRunning(true);
	}

	public final CooldownTimer getTimer() {
		return timer;
	}

	public final boolean isAlive() {
		return timer.isAvailable();
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getCooldown() {
		return cooldown;
	}

	public long getTreshhold() {
		return threshhold;
	}

	public boolean isTriggerable() {
		return threshhold <= 0;
	}

	public void trigger() {
		threshhold = cooldown;
	}

	private void decrement() {
		threshhold--;
	}

	private void reset() {
		threshhold = 0;
	}

	public static class CooldownTimer extends Thread {

		private boolean alive = true;
		private boolean running = false;

		private Cooldown cooldown;

		public CooldownTimer(Cooldown delayer) {
			this.cooldown = delayer;
			setDaemon(true);
			start();
		}

		@Override
		public void run() {
			while (alive) {
				while (running) {
					try {
						if (!cooldown.isTriggerable()) {
							cooldown.decrement();
						}
						Thread.sleep(1);
					} catch (Throwable e) {
						cooldown.reset();
					}
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// Idk, just ignore
				}
			}
		}

		public void setRunning(boolean running) {
			this.running = running;
		}

		public boolean isRunning() {
			return running;
		}

		public void kill() {
			alive = false;
		}

		public boolean isAvailable() {
			return alive;
		}

	}

}
