package com.example.dormitoryrepair.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeRangeParserTest {

    @Test
    void parseSingleDateExpandsToWholeDay() {
        LocalDateTime[] range = DateTimeRangeParser.parse("2026-04-27");

        assertEquals(LocalDateTime.of(2026, 4, 27, 0, 0, 0), range[0]);
        assertEquals(LocalDateTime.of(2026, 4, 27, 23, 59, 59), range[1]);
    }

    @Test
    void parseDateRangeUsesBoundaryDays() {
        LocalDateTime[] range = DateTimeRangeParser.parse("2026-04-01,2026-04-03");

        assertEquals(LocalDateTime.of(2026, 4, 1, 0, 0, 0), range[0]);
        assertEquals(LocalDateTime.of(2026, 4, 3, 23, 59, 59), range[1]);
    }
}
