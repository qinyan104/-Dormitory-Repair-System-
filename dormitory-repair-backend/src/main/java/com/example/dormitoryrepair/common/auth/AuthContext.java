package com.example.dormitoryrepair.common.auth;

public final class AuthContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static void setRole(String role) {
        ROLE_HOLDER.set(role);
    }

    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String getRole() {
        return ROLE_HOLDER.get();
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}
