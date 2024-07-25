package de.ngloader.referee.module.registry.schoolplan;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import de.ngloader.referee.RefereeLogger;

public class SchoolplanThread {
	
	private static final long UPDATE_DELAY = 30 * 60 * 1000;

	private final AtomicBoolean running = new AtomicBoolean(false);
	
	private final SchoolplanModule module;
	
	private Thread thread;
	private int failedRequestCount = 0;
	
	private AtomicLong nextUpdateTime = new AtomicLong();

	public SchoolplanThread(SchoolplanModule module) {
		this.module = module;
	}

	private void run() {
		while(this.running.get()) {
			try {
				Thread.sleep(1000); // check for updates every minute
			} catch (InterruptedException e) {
				// ignore interrupted exception
				RefereeLogger.info("Schoolplan Scheduler was interrupted");
				return;
			} catch (Exception e) {
				RefereeLogger.error("Error in tick task from plan scheduler! stopping task", e);
				this.destroy();
				return;
			}
			
			long updateTime = this.nextUpdateTime.get();
			if (System.currentTimeMillis() < updateTime) {
				continue;
			}
			this.resetUpdateTime();
			
			try {
				RefereeLogger.info("Schoolplan fetch latest version...");
				
				this.module.updatePlan(false);
			} catch (Exception e) {
				RefereeLogger.error(String.format("Something went wrong! Try (%d/30)", this.failedRequestCount++), e);
				
				if (this.failedRequestCount > 30) {
					RefereeLogger.warn("Stopping tick task from plan scheduler because alle 30 request failed!");
					this.destroy();
					
					for (ScheduleClass clazz : this.module.getClasses()) {
						clazz.channel().createMessage(String.format("<@%s> Stundenplan scheduler wegen störungen pausiert! Bitte logs überprüfen!",
								this.module.getAdminRoleId())
							).subscribe();
					}
					return;
				}
			}
		}
		
		RefereeLogger.info("Schoolplan scheduler finished running.");
	}
	
	public void resetUpdateTime() {
		this.nextUpdateTime.set(System.currentTimeMillis() + UPDATE_DELAY);
	}
	
	public void forceUpdate() {
		this.nextUpdateTime.set(0);
	}
	
	public void start() {
		if (this.running.compareAndSet(false, true)) {
			this.thread = new Thread(this::run, "Referee - Schoolplan - Scheduler");
			this.thread.start();
		}
	}
	
	public void destroy() {
		this.running.compareAndSet(true, false);
		
		if (this.thread != null && this.thread.isAlive()) {
			this.thread.interrupt();
		}
		this.thread = null;
	}
}
