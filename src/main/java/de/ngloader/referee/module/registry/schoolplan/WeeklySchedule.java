package de.ngloader.referee.module.registry.schoolplan;

import java.util.List;

public record WeeklySchedule(String date, String course, String teacher, List<DailySchedule> daily) {

}
