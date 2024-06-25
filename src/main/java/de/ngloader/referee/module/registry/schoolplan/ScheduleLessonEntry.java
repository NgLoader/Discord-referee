package de.ngloader.referee.module.registry.schoolplan;

public record ScheduleLessonEntry(String teacher, String course, String room, String addition, int index) implements ScheduleLesson {

}
