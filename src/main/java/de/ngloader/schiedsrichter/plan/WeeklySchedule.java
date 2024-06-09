package de.ngloader.schiedsrichter.plan;

import java.util.List;

public record WeeklySchedule(String date, String course, String teacher, List<DailySchedule> daily) {

}
