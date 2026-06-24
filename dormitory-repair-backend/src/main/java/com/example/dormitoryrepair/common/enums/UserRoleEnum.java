package com.example.dormitoryrepair.common.enums;

/**
 * 系统用户角色枚举.
 */
public enum UserRoleEnum {

    ADMIN("ADMIN", "管理员"),
    STUDENT("STUDENT", "学生"),
    REPAIRER("REPAIRER", "维修人员");

    private final String code;
    private final String label;

    UserRoleEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static UserRoleEnum fromCode(String code) {
        if (code == null) return null;
        for (UserRoleEnum role : values()) {
            if (role.code.equals(code)) return role;
        }
        return null;
    }
}
