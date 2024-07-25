package de.ngloader.referee.module.registry;

import java.util.List;

import de.ngloader.referee.Referee;
import de.ngloader.referee.command.RefereeCommand;
import de.ngloader.referee.module.RefereeModule;

public class ExamModule extends RefereeModule {

	public ExamModule(Referee app) {
		super(app, "Exam");
	}

	@Override
	protected void onInitalize() {
	}

	@Override
	protected void onEnable() {
	}

	@Override
	protected void onDisable() {
	}

	@Override
	protected List<RefereeCommand> registerCommands() {
		return List.of();
	}
}
