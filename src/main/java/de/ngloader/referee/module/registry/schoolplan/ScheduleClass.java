package de.ngloader.referee.module.registry.schoolplan;

import discord4j.core.object.entity.channel.TextChannel;

public record ScheduleClass(String name, String tagId, TextChannel channel) {
}
