package com.example.dormitoryrepair.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PageResultUtil {

    private PageResultUtil() {
    }

    public static <T> Map<String, Object> build(List<T> allRecords, Integer pageNum, Integer pageSize) {
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        List<T> records = allRecords == null ? Collections.emptyList() : allRecords;
        int fromIndex = Math.min((current - 1) * size, records.size());
        int toIndex = Math.min(fromIndex + size, records.size());

        Map<String, Object> result = new HashMap<>();
        result.put("records", fromIndex >= toIndex ? Collections.emptyList() : new ArrayList<>(records.subList(fromIndex, toIndex)));
        result.put("total", (long) records.size());
        result.put("current", (long) current);
        result.put("size", (long) size);
        return result;
    }
}
