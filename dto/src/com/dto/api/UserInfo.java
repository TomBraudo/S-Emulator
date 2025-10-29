package com.dto.api;

public class UserInfo {
    private final String name;
    private final int programUploadedCount;
    private final int functionUploadedCount;
    private final int credits;
    private final int creditsUsed;
    private final int runCount;

    public UserInfo(String name, int programUploadedCount, int functionUploadedCount, int credits, int creditsUsed, int runCount) {
        this.name = name;
        this.programUploadedCount = programUploadedCount;
        this.functionUploadedCount = functionUploadedCount;
        this.credits = credits;
        this.creditsUsed = creditsUsed;
        this.runCount = runCount;
    }
    public String getName() { return name; }
    public int getProgramUploadedCount() { return programUploadedCount; }
    public int getFunctionUploadedCount() { return functionUploadedCount; }
    public int getCredits() { return credits; }
    public int getCreditsUsed() { return creditsUsed; }
    public int getRunCount() { return runCount; }
}
