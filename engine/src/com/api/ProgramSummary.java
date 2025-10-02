package com.api;

public class ProgramSummary {
    int baseCommands;
    int syntheticCommands;

    public ProgramSummary(int baseCommands, int syntheticCommands) {
        this.baseCommands = baseCommands;
        this.syntheticCommands = syntheticCommands;
    }
    public int getBaseCommands() {
        return baseCommands;
    }
    public int getSyntheticCommands() {
        return syntheticCommands;
    }
}
