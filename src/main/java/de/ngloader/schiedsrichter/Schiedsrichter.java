package de.ngloader.schiedsrichter;

import de.ngloader.schiedsrichter.plan.SchoolplanModule;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;

public class Schiedsrichter {

	public static void main(String[] args) {
		new Schiedsrichter(args[0]);
	}

	private final DiscordClient client;
	private final GatewayDiscordClient gateway;

	private final Guild guild;

	public Schiedsrichter(String token) {
		this.client = DiscordClient.create(token);
		this.gateway = this.client.login().block();

		this.guild = this.gateway.getGuildById(Snowflake.of("1248302583039262944")).block();

		new SchoolplanModule(this);

		this.gateway.onDisconnect().block();
	}

	public void destroy() {
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
}
