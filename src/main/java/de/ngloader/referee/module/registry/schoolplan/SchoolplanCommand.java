package de.ngloader.referee.module.registry.schoolplan;

import java.util.List;

import de.ngloader.referee.command.RefereeCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class SchoolplanCommand implements RefereeCommand {
	
	private SchoolplanModule module;
	
	SchoolplanCommand(SchoolplanModule module) {
		this.module = module;
	}

	@Override
	public String getCommand() {
		return "plan";
	}

	@Override
	public ApplicationCommandRequest createApplication() {
		return ApplicationCommandRequest.builder()
				.name("plan")
				.description("Studenplan verwalten")
				.addAllOptions(List.of(
						ApplicationCommandOptionData.builder()
						.name("validate")
						.description("Überprüfe ob es einen neuen Stundenplan gibt")
						.build(),
						ApplicationCommandOptionData.builder()
						.name("generate")
						.description("Generiere einen neuen Stundenplan")
						.build())
				).build();
	}

	@Override
	public Mono<Message> handle(ChatInputInteractionEvent event) {
		return null;
	}

}
