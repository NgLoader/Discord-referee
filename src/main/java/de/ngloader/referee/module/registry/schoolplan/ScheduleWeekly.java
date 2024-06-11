package de.ngloader.referee.module.registry.schoolplan;

import java.util.List;

public record ScheduleWeekly(String date, String course, String teacher, List<ScheduleDaily> daily) {

}
