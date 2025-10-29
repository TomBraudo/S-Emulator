package com.app.ui.dashboard;

public final class UserContext {
    private static volatile String userId;
    private static volatile Integer credits;

    private UserContext() {}

    public static String getUserId() {
        return userId;
    }

    public static void setUserId(String id) {
        userId = id;
    }

    public static Integer getCredits() {
        return credits;
    }

    public static void setCredits(Integer value) {
        credits = value;
    }
}


