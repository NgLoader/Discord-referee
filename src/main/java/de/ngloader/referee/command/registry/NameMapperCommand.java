package de.ngloader.referee.command.registry;

import java.util.List;

import de.ngloader.referee.command.RefereeCommand;
import de.ngloader.referee.util.NameMapper;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class NameMapperCommand implements RefereeCommand {

	@Override
	public String getCommand() {
		return "NameMapper";
	}

	@Override
	public ApplicationCommandRequest createApplication() {
		return ApplicationCommandRequest.builder()
				.name("namemapper")
				.description("Name mapper verwalten")
				.addAllOptions(List.of(
						ApplicationCommandOptionData.builder()
						.name("reload")
						.description("Lade alle name mappings neu")
				        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
				        .build()
					)
				).build();
	}

	@Override
	public Mono<Message> handle(ChatInputInteractionEvent event) {
		NameMapper.reloadAll();
		
		return event.createFollowup("Alle name mappings wurden neugeladen!");
	}
}
