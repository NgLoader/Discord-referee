package de.ngloader.referee.module;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.ngloader.referee.Referee;
import de.ngloader.referee.command.RefereeCommand;

public abstract class RefereeModule {

	protected final Referee app;
	protected final String name;

	private final AtomicBoolean enabled = new AtomicBoolean(false);
	
	private final List<RefereeCommand> commands = this.registerCommands();

	public RefereeModule(Referee app, String name) {
		this.app = app;
		this.name = name.replace(" ", "_");
	}

	protected abstract void onInitalize();
	protected abstract void onEnable();
	protected abstract void onDisable();

	protected abstract List<RefereeCommand> registerCommands();

	void initalize() {
		this.onInitalize();
	}

	void enable() {
		if (this.enabled.compareAndSet(false, true)) {
			this.onEnable();
		}
	}

	void disable() {
		if (this.enabled.compareAndSet(true, false)) {
			this.onDisable();
		}
	}

	public boolean isEnabled() {
		return this.enabled.get();
	}
	
	public String getName() {
		return this.name;
	}
	
	public List<RefereeCommand> getCommands() {
		return this.commands;
	}
}