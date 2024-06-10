package de.ngloader.referee;

import de.ngloader.referee.module.registry.schoolplan.SchoolplanModule;
import de.ngloader.referee.util.CourseName;
import de.ngloader.referee.util.TeacherName;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.rest.service.ApplicationService;

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
		
		CourseName.initalizeStaticFields();
		TeacherName.initalizeStaticFields();
		
		new Referee(config);
	}

	private final RefereeConfig config;
	
	private final DiscordClient client;
	private final GatewayDiscordClient gateway;

	private final Guild guild;
	
	private final SchoolplanModule schoolplanModule;

	public Referee(RefereeConfig config) {
		this.config = config;
		
		this.client = DiscordClient.create(config.getBotToken());
		this.gateway = this.client.login().block();

		this.guild = this.gateway.getGuildById(config.getGuildId()).block();

		this.schoolplanModule = new SchoolplanModule(this);
		this.schoolplanModule.startScheduler();

		this.gateway.onDisconnect().block();
	}
	
	public void registerCommands() {
		long guildId = this.guild.getId().asLong();
		long applicationId = this.client.getApplicationId().block();
		ApplicationService applicationService = this.client.getApplicationService();
		
		applicationService.createGuildApplicationCommand(applicationId, guildId, this.schoolplanModule.createCommand()).subscribe();
	}

	public void destroy() {
		this.schoolplanModule.destroy();
		
		this.gateway.logout().block();
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
