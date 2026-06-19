package com.example.dormitoryrepair.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeRangeParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeRangeParser() {
    }

    public static LocalDateTime[] parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] parts = text.trim().split("\\s*(?:,|~|至)\\s*");
        if (parts.length == 1) {
            return single(parts[0]);
        }
        if (parts.length == 2) {
            return range(parts[0], parts[1]);
        }
        throw new IllegalArgumentException("时间范围格式不正确");
    }

    private static LocalDateTime[] single(String value) {
        if (value.length() == 10) {
            LocalDate date = LocalDate.parse(value);
            return new LocalDateTime[]{date.atStartOfDay(), date.atTime(23, 59, 59)};
        }
        LocalDateTime dateTime = parseDateTime(value);
        return new LocalDateTime[]{dateTime, dateTime};
    }

    private static LocalDateTime[] range(String start, String end) {
        LocalDateTime startTime = start.length() == 10
                ? LocalDate.parse(start).atStartOfDay()
                : parseDateTime(start);
        LocalDateTime endTime = end.length() == 10
                ? LocalDate.parse(end).atTime(23, 59, 59)
                : parseDateTime(end);
        return new LocalDateTime[]{startTime, endTime};
    }

    private static LocalDateTime parseDateTime(String value) {
        try {
            String normalized = value.replace("T", " ");
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("时间格式不正确，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}
