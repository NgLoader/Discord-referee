package de.ngloader.referee.command.registry;

import de.ngloader.referee.Referee;
import de.ngloader.referee.command.RefereeCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class PlanCommand implements RefereeCommand {

	private final Referee app;

	public PlanCommand(Referee app) {
		this.app = app;
	}
	
	@Override
	public String getCommand() {
		return "plan";
	}

	@Override
	public ApplicationCommandRequest createApplication() {
		return null;
	}

	@Override
	public Mono<Message> handle(ChatInputInteractionEvent event) {
		// TODO Auto-generated method stub
		return null;
	}

}
