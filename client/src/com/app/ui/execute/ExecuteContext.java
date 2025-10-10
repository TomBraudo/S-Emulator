package com.app.ui.execute;

public final class ExecuteContext {
    private static volatile String programName;
    private static volatile String functionName;
    private static volatile String username;
    private static volatile int credits;

    private ExecuteContext() {}

    public static String getProgramName() {
        return programName;
    }

    public static void setProgramName(String name) {
        programName = name;
    }

    public static String getFunctionName() {
        return functionName;
    }

    public static void setFunctionName(String name) {
        functionName = name;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String name) {
        username = name;
    }

    public static int getCredits() {
        return credits;
    }

    public static void setCredits(int value) {
        credits = value;
    }

    public static String getName(){
        return programName == null ? functionName : programName;
    }

    public static void clear() {
        programName = null;
        functionName = null;
        username = null;
        credits = 0;
    }
}

