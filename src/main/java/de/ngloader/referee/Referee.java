package de.ngloader.referee;

import de.ngloader.referee.command.CommandManager;
import de.ngloader.referee.module.ModuleManager;
import de.ngloader.referee.util.NameMapper;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;

public class Referee {

	public static void main(String[] args) {
		RefereeConfig config = new RefereeConfig();
		if (config.loadConfig()) {
			RefereeLogger.info("Config successful loaded!");
		} else {
			RefereeLogger.warn("Error loading config!");
			return;
		}
		
		// save when new variables where added (updates)
		config.saveConfig();
		
		NameMapper.initalizeStaticFields();
		
		new Referee(config);
	}

	private final RefereeConfig config;
	
	private final DiscordClient client;
	private final GatewayDiscordClient gateway;

	private final Guild guild;
	
	private final CommandManager commandManager;
	private final ModuleManager moduleManager;

	public Referee(RefereeConfig config) {
		this.config = config;
		
		this.client = DiscordClient.create(config.getBotToken());
		this.gateway = this.client.login().block();

		this.guild = this.gateway.getGuildById(config.getGuildId()).block();

		this.commandManager = new CommandManager(this);
		this.moduleManager = new ModuleManager(this);

		this.commandManager.overrideApplicationCommands();

		this.gateway.onDisconnect().block();
	}

	public void destroy() {
		this.commandManager.destroy();
		this.moduleManager.destroy();
		
		this.gateway.logout().block();
	}
	
	public CommandManager getCommandManager() {
		return this.commandManager;
	}
	
	public ModuleManager getModuleManager() {
		return this.moduleManager;
	}

	public Guild getGuild() {
		return this.guild;
	}

	public DiscordClient getClient() {
		return this.client;
	}

	public GatewayDiscordClient getGateway() {
		return this.gateway;
	}
	
	public RefereeConfig getConfig() {
		return this.config;
	}
}
