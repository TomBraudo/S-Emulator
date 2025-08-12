package com.commands;

import com.program.ProgramState;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Neutral extends BaseCommand {
    String variableName;
    Neutral(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        cycles = 0;
    }
    @Override
    public void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (B) [ %s ] %s <- %s (%d)", index+1, displayLabel(), variableName, variableName, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    public HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        return List.of(new Neutral(variableName, label, realIndex.getAndIncrement(), creator));
    }

    @Override
    public int getExpansionLevel() {
        return 0;
    }

    @Override
    public String getTargetLabel() {
        return NO_LABEL;
    }
}