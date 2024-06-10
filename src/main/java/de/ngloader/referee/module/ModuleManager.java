package de.ngloader.referee.module;

import java.util.HashMap;
import java.util.Map;

import de.ngloader.referee.Referee;

public class ModuleManager {

	private Map<String, RefereeModule> modules = new HashMap<>();
	
	public ModuleManager(Referee app) {
		
	}
	
	private void registerModule(RefereeModule module) {
		this.modules.put(module.getName().toLowerCase(), module);
	}
}
