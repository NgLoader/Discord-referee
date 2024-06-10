package de.ngloader.referee.command;

import java.util.HashMap;
import java.util.Map;

import de.ngloader.referee.Referee;
import de.ngloader.referee.RefereeLogger;
import de.ngloader.referee.command.registry.NameMapperCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.rest.service.ApplicationService;
import reactor.core.publisher.Mono;

public class CommandManager {
	
	private final Map<String, RefereeCommand> registeredCommands = new HashMap<>();

	private final Referee app;
	
	public CommandManager(Referee app) {
		this.app = app;
		
		this.registerCommand(new NameMapperCommand());
		
		app.getGateway().on(ChatInputInteractionEvent.class, this::handleChatInput).subscribe();
	}
	
	private Mono<Message> handleChatInput(ChatInputInteractionEvent event) {
		RefereeCommand command = this.registeredCommands.get(event.getCommandName().toLowerCase());
		if (command != null) {
			return event.deferReply()
					.withEphemeral(command.ephemeral())
					.then(this.handleCommand(event, command));
		}
		return null;
	}
	
	private Mono<Message> handleCommand(ChatInputInteractionEvent event, RefereeCommand command) {
		try {
			return command.handle(event);
		} catch (Exception e) {
			RefereeLogger.error(String.format("Error by handling command \"%s\"", command.getCommand()), e);
			return event.createFollowup("A error occured by executing the command!");
		}
	}
	
	// TODO apply when new commands are registered
	public void overrideApplicationCommands() {
		RefereeLogger.info("Command override guild application commands");
		
		long guildId = this.app.getGuild().getId().asLong();
		long applicationId = this.app.getClient().getApplicationId().block();
		ApplicationService applicationService = this.app.getClient().getApplicationService();
		
		applicationService.bulkOverwriteGuildApplicationCommand(applicationId, guildId, this.registeredCommands.values().stream()
				.distinct()
				.map(command -> command.createApplication())
				.toList()).subscribe();
	}
	
	public void registerCommand(RefereeCommand command) {
		RefereeLogger.info("Command " + command.getCommand() + " registered.");
		this.registeredCommands.put(command.getCommand().toLowerCase(), command);
	}
	
	public void unregisterCommand(RefereeCommand command) {
		RefereeLogger.info("Command " + command.getCommand() + " unregistered.");
		this.registeredCommands.remove(command.getCommand().toLowerCase());
	}
	
	public void destroy() {
		
	}
}
