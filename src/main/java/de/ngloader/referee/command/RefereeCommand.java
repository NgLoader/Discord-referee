package de.ngloader.referee.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface RefereeCommand {

	String getCommand();
	
	ApplicationCommandRequest createApplication();
	
	Mono<Message> handle(ChatInputInteractionEvent event);

	default boolean ephemeral() {
		return true;
	}

	default void destroy() { }
}
