package com.app.ui.dashboard;

public final class UserContext {
    private static volatile String userId;

    private UserContext() {}

    public static String getUserId() {
        return userId;
    }

    public static void setUserId(String id) {
        userId = id;
    }
}


