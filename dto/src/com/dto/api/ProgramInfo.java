package com.dto.api;

import java.io.Serializable;

public class ProgramInfo implements Serializable {
    private String name;
    private String owner;
    private int commandsCount;
    private int maxLevel;
    private int ranCount;
    private double averageCost;
    private String sourceProgram; // only for functions; null for programs
    private boolean function;     // true if helper function, false if program

    public ProgramInfo(String name, String owner, int commandsCount, int maxLevel, int ranCount, double averageCost, String sourceProgram, boolean function) {
        this.name = name;
        this.owner = owner;
        this.commandsCount = commandsCount;
        this.maxLevel = maxLevel;
        this.ranCount = ranCount;
        this.averageCost = averageCost;
        this.sourceProgram = sourceProgram;
        this.function = function;
    }

    public String getName() { return name; }
    public String getOwner() { return owner; }
    public int getCommandsCount() { return commandsCount; }
    public int getMaxLevel() { return maxLevel; }
    public int getRanCount() { return ranCount; }
    public double getAverageCost() { return averageCost; }
    public String getSourceProgram() { return sourceProgram; }
    public boolean isFunction() { return function; }
}


