package com.dto.api;

import java.util.List;

public class ProgramSummary {
    List<Integer> architectureCommandsCount;

    public ProgramSummary(List<Integer> architectureCommandsCount) {
        this.architectureCommandsCount = architectureCommandsCount;
    }
    public int getArchitectureCommandsCount(int architectureIndex) {
        return architectureCommandsCount.get(architectureIndex);
    }
}


