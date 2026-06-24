package com.example.dormitoryrepair.common.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报修工单状态枚举 (6-state flow).
 * 1待受理 → 2已派单 → 3维修中 → 4待确认 → 5已完成 → 6已取消
 */
public enum RepairStatusEnum {

    PENDING(1, "待受理"),
    ASSIGNED(2, "已派单"),
    IN_PROGRESS(3, "维修中"),
    AWAITING_CONFIRM(4, "待确认"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String label;

    RepairStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    private static final Map<Integer, RepairStatusEnum> BY_CODE =
            Arrays.stream(values()).collect(Collectors.toMap(RepairStatusEnum::getCode, s -> s));

    public static RepairStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        return BY_CODE.get(code);
    }

    /** Valid transitions matrix — maps current status to set of allowed target statuses. */
    public static final Map<Integer, Set<Integer>> VALID_TRANSITIONS = Map.of(
            1, Set.of(2, 6),
            2, Set.of(3, 6),
            3, Set.of(4, 6),
            4, Set.of(5, 6),
            5, Set.of(),
            6, Set.of()
    );

    public static boolean isValidTransition(int currentStatus, int targetStatus) {
        Set<Integer> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /** Lookup a human-readable label for a status code. */
    public static String statusLabel(Integer code) {
        RepairStatusEnum s = fromCode(code);
        return s != null ? s.getLabel() : "未知";
    }
}
