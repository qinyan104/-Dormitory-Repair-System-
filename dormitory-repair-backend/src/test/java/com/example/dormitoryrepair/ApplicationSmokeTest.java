package com.example.dormitoryrepair;

import com.example.dormitoryrepair.common.enums.RepairStatusEnum;
import com.example.dormitoryrepair.common.enums.UserRoleEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight utility-level smoke tests that verify core framework components
 * without requiring a Spring application context, database, or Redis.
 */
class ApplicationSmokeTest {

    // ==================== Enum tests ====================

    @Test
    void repairStatusEnum_ValidTransitions() {
        assertTrue(RepairStatusEnum.isValidTransition(1, 2));
        assertTrue(RepairStatusEnum.isValidTransition(1, 6));
        assertTrue(RepairStatusEnum.isValidTransition(2, 3));
        assertTrue(RepairStatusEnum.isValidTransition(3, 4));
        assertTrue(RepairStatusEnum.isValidTransition(4, 5));
        assertFalse(RepairStatusEnum.isValidTransition(5, 6));
        assertFalse(RepairStatusEnum.isValidTransition(1, 5));
    }

    @Test
    void repairStatusEnum_FromCode() {
        assertEquals("待受理", RepairStatusEnum.fromCode(1).getLabel());
        assertEquals("已完成", RepairStatusEnum.fromCode(5).getLabel());
        assertEquals("已取消", RepairStatusEnum.fromCode(6).getLabel());
        assertNull(RepairStatusEnum.fromCode(99));
        assertNull(RepairStatusEnum.fromCode(null));
    }

    @Test
    void userRoleEnum_FromCode() {
        assertEquals(UserRoleEnum.ADMIN, UserRoleEnum.fromCode("ADMIN"));
        assertEquals(UserRoleEnum.STUDENT, UserRoleEnum.fromCode("STUDENT"));
        assertEquals(UserRoleEnum.REPAIRER, UserRoleEnum.fromCode("REPAIRER"));
        assertNull(UserRoleEnum.fromCode("INVALID"));
        assertNull(UserRoleEnum.fromCode(null));
    }

    @Test
    void userRoleEnum_GetCode() {
        assertEquals("ADMIN", UserRoleEnum.ADMIN.getCode());
        assertEquals("STUDENT", UserRoleEnum.STUDENT.getCode());
        assertEquals("REPAIRER", UserRoleEnum.REPAIRER.getCode());
    }

    // ==================== Critical constants ====================

    @Test
    void repairStateMachine_CompleteFlow() {
        // Full happy path: 1 → 2 → 3 → 4 → 5
        assertTrue(RepairStatusEnum.isValidTransition(1, 2));
        assertTrue(RepairStatusEnum.isValidTransition(2, 3));
        assertTrue(RepairStatusEnum.isValidTransition(3, 4));
        assertTrue(RepairStatusEnum.isValidTransition(4, 5));
    }

    @Test
    void repairStateMachine_CancelAnywhere() {
        // Cancellation allowed from status 1-4
        assertTrue(RepairStatusEnum.isValidTransition(1, 6));
        assertTrue(RepairStatusEnum.isValidTransition(2, 6));
        assertTrue(RepairStatusEnum.isValidTransition(3, 6));
        assertTrue(RepairStatusEnum.isValidTransition(4, 6));
        // Cannot cancel after completed
        assertFalse(RepairStatusEnum.isValidTransition(5, 6));
        assertFalse(RepairStatusEnum.isValidTransition(6, 1));
    }

    @Test
    void repairStateMachine_NoIllegalJumps() {
        assertFalse(RepairStatusEnum.isValidTransition(1, 5));
        assertFalse(RepairStatusEnum.isValidTransition(2, 5));
        assertFalse(RepairStatusEnum.isValidTransition(1, 3));
        assertFalse(RepairStatusEnum.isValidTransition(2, 4));
    }
}
