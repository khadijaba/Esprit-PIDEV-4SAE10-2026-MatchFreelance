package com.freelancing.productivity.service;

import com.freelancing.productivity.entity.ProductivityTask;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class IcsUtils {

    private static final DateTimeFormatter ICS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private IcsUtils() {
    }

    public static String tasksToIcs(String calendarName, List<ProductivityTask> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//MatchFreelance//Productivity//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("X-WR-CALNAME:").append(escape(calendarName)).append("\r\n");

        tasks.stream().filter(t -> t.getDueAt() != null).forEach(task -> {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:task-").append(task.getId()).append("@matchfreelance\r\n");
            sb.append("DTSTAMP:").append(ICS_FORMAT.format(task.getUpdatedAt())).append("\r\n");
            sb.append("DTSTART:").append(ICS_FORMAT.format(task.getDueAt())).append("\r\n");
            sb.append("DTEND:").append(ICS_FORMAT.format(task.getDueAt().plusSeconds(1800))).append("\r\n");
            sb.append("SUMMARY:").append(escape(task.getTitle())).append("\r\n");
            sb.append("DESCRIPTION:").append(escape(task.getDescription() == null ? "" : task.getDescription())).append("\r\n");
            sb.append("STATUS:").append(toIcsStatus(task.getStatus().name())).append("\r\n");
            sb.append("END:VEVENT\r\n");
        });

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String toIcsStatus(String status) {
        if ("DONE".equals(status)) {
            return "CONFIRMED";
        }
        return "TENTATIVE";
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}


