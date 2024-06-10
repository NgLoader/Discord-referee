package de.ngloader.referee.command.registry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import de.ngloader.referee.command.RefereeCommand;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class PurgeCommand implements RefereeCommand {

	@Override
	public String getCommand() {
		return "Purge";
	}

	@Override
	public ApplicationCommandRequest createApplication() {
		return ApplicationCommandRequest.builder()
				.name("purge")
				.description("Delete channel messages by count")
				.addAllOptions(List.of(
						ApplicationCommandOptionData.builder()
						.name("count")
						.description("Delete count")
				        .type(ApplicationCommandOption.Type.INTEGER.getValue())
				        .required(true)
				        .build()
					)
				).build();
	}

	@Override
	public Mono<Message> handle(ChatInputInteractionEvent event) {
		Optional<ApplicationCommandInteractionOption> optionalCount = event.getOption("count");
		if (optionalCount.isEmpty()) {
			return event.createFollowup("Missing count variable!");
		}
		
		Optional<ApplicationCommandInteractionOptionValue> optionalValue = optionalCount.get().getValue();
		if (optionalValue.isEmpty()) {
			return event.createFollowup("Invalid delete count!");
		}
		int count = (int) optionalValue.get().asLong();
		if (count > 50) {
			return event.createFollowup("Bitte gebe eine zahl zwischen eins und 50 ein!");
		}

		User user = event.getInteraction().getUser();
		MessageChannel channel = event.getInteraction().getChannel().block();
		
		List<Message> messageList = channel.getMessagesBefore(Snowflake.of(Instant.now())).collectList().block();		
		count = messageList.size() > count ? count : messageList.size();
		
		int index = 0;
		while (count > index) {
			Message message = messageList.get(index);
			index++;
			
			message.delete(String.format("Referee - Pruge - Executer (%s) %s",
					user.getUsername(),
					user.getId().asString()))
				.subscribe();
		}
		
		return event.createFollowup("Lösche " + count + " nachrichten...");
	}
}
