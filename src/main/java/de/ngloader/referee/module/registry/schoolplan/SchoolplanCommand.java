package de.ngloader.referee.module.registry.schoolplan;

import java.util.List;

import de.ngloader.referee.command.RefereeCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
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
		return "Plan";
	}

	@Override
	public ApplicationCommandRequest createApplication() {
		return ApplicationCommandRequest.builder()
				.name("plan")
				.description("Studenplan verwalten")
				.addAllOptions(List.of(
						ApplicationCommandOptionData.builder()
						.name("generate")
						.description("Generiere einen neuen Stundenplan")
				        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
				        .build(),
						ApplicationCommandOptionData.builder()
						.name("check")
						.description("Prüfe ob ein neuer stundenplan vorhanden ist")
				        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
						.build()
					)
				).build();
	}

	@Override
	public Mono<Message> handle(ChatInputInteractionEvent event) {
		ApplicationCommandInteractionOption option = event.getOptions().getFirst();
		switch (option.getName()) {
			case "generate":
				this.module.forceUpdate(true);
				return event.createFollowup("Neuer Stundenplan wird dargestellt!");
				
			case "check":
				this.module.forceUpdate(false);
				return event.createFollowup("Der Stundenplan wird auf aktualisierungen geprüft!");
			
			default:
				return event.createFollowup("Invalid action!");
		}
	}

}
