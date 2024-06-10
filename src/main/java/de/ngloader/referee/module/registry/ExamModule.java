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
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onEnable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onDisable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected List<RefereeCommand> registerCommands() {
		// TODO Auto-generated method stub
		return List.of();
	}

}
