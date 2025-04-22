package de.ngloader.referee.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ngloader.referee.Referee;
import de.ngloader.referee.RefereeLogger;
import de.ngloader.referee.command.CommandManager;
import de.ngloader.referee.command.RefereeCommand;
import de.ngloader.referee.module.registry.ExamModule;
import de.ngloader.referee.module.registry.VerifierModule;
import de.ngloader.referee.module.registry.schoolplan.SchoolplanModule;

public class ModuleManager {

	private Map<String, RefereeModule> modules = new HashMap<>();

	private final CommandManager commandManager;

	public ModuleManager(Referee app) {
		this.commandManager = app.getCommandManager();
		
		this.registerModule(new SchoolplanModule(app));
		this.registerModule(new VerifierModule(app));
		this.registerModule(new ExamModule(app));
//		this.registerModule(new DownloadAllModule(app));
		
		this.modules.values().forEach(RefereeModule::initalize);
		
		this.enableAllModules();
	}

	private void registerModule(RefereeModule module) {
		this.modules.put(module.getName().toLowerCase(), module);
		RefereeLogger.info("Module " + module.getName() + " registered.");
	}
	
	public void enableAllModules() {
		RefereeLogger.info("Module enabling all modules...");
		this.modules.values().forEach(this::enableModule);
		RefereeLogger.info("Module enabled all modules.");
	}
	
	public void disableAllModules() {
		RefereeLogger.info("Module disabling all modules...");
		this.modules.values().forEach(this::disableModule);
		RefereeLogger.info("Module disabled all modules.");
	}
	
	public void enableModule(RefereeModule module) {
		RefereeLogger.info("Module " + module.getName() + " enabling...");
		module.enable();
		
		List<RefereeCommand> commands = module.getCommands();
		commands.forEach(this.commandManager::registerCommand);
		RefereeLogger.info("Module " + module.getName() + " enabled.");
	}
	
	public void disableModule(RefereeModule module) {
		RefereeLogger.info("Module " + module.getName() + " disabling...");
		module.disable();
		
		List<RefereeCommand> commands = module.getCommands();
		commands.forEach(this.commandManager::unregisterCommand);
		RefereeLogger.info("Module " + module.getName() + " disabled.");
	}

	public void destroy() {
		this.disableAllModules();
	}
}
