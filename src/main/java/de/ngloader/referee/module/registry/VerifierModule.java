package de.ngloader.referee.module.registry;

import java.util.List;

import de.ngloader.referee.Referee;
import de.ngloader.referee.command.RefereeCommand;
import de.ngloader.referee.module.RefereeModule;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class VerifierModule extends RefereeModule implements RefereeCommand {

	public VerifierModule(Referee app) {
		super(app, "Verifier");
	}

	@Override
	protected void onInitalize() {
	}

	@Override
	protected void onEnable() {
	}

	@Override
	protected void onDisable() {
	}

	@Override
	protected List<RefereeCommand> registerCommands() {
		return List.of(this);
	}

	@Override
	public String getCommand() {
		return "Verify";
	}

	@Override
	public ApplicationCommandRequest createApplication() {
		return ApplicationCommandRequest.builder()
				.name("verify")
				.addAllOptions(List.of(
						ApplicationCommandOptionData.builder()
						.name("user")
						.description("Todo")
				        .type(ApplicationCommandOption.Type.USER.getValue())
				        .build()
					)
				).build();
	}

	@Override
	public Mono<Message> handle(ChatInputInteractionEvent event) {
		ApplicationCommandInteractionOption option = event.getOption("user").get();
		User user = option.getValue().get().asUser().block();
		return event.createFollowup("ok, " + user.getUsername());
	}
}
