package de.ngloader.referee.command;

import java.util.HashMap;
import java.util.Map;

import de.ngloader.referee.Referee;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.rest.service.ApplicationService;
import reactor.core.publisher.Mono;

public class CommandManager {
	
	private final Map<String, RefereeCommand> registeredCommands = new HashMap<>();

	private final Referee app;
	
	public CommandManager(Referee app) {
		this.app = app;
		
		this.overrideApplicationCommands();
		this.registerCommand(null);
		
		app.getGateway().on(ChatInputInteractionEvent.class, this::handleChatInput).subscribe();
	}
	
	private Mono<Message> handleChatInput(ChatInputInteractionEvent event) {
		RefereeCommand command = this.registeredCommands.get(event.getCommandName().toLowerCase());
		if (command != null) {
			return event.deferReply()
					.withEphemeral(true)
					.then(this.handleCommand(event, command));
		}
		return null;
	}
	
	private Mono<Message> handleCommand(ChatInputInteractionEvent event, RefereeCommand command) {
		return command.handle(event);
	}
	
	private void overrideApplicationCommands() {
		long guildId = this.app.getGuild().getId().asLong();
		long applicationId = this.app.getClient().getApplicationId().block();
		ApplicationService applicationService = this.app.getClient().getApplicationService();
		
		applicationService.bulkOverwriteGuildApplicationCommand(applicationId, guildId, this.registeredCommands.values().stream()
				.distinct()
				.map(command -> command.createApplication())
				.toList());
	}
	
	public void registerCommand(RefereeCommand command) {
		this.registeredCommands.put(command.getCommand().toLowerCase(), command);
	}
}
