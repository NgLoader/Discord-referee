package de.ngloader.referee.module;

import java.util.List;

import de.ngloader.referee.command.RefereeCommand;

public interface RefereeModule {

	String getName();
	
	default List<RefereeCommand> registerCommands() {
		return List.of();
	}
}