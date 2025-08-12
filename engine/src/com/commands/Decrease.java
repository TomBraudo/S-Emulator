package com.commands;

import com.program.ProgramState;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Decrease extends BaseCommand {
    String variableName;
    Decrease(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        cycles = 1;
    }

    @Override
    public void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        if (v.getValue() > 0) {
            v.setValue(v.getValue() - 1);
        }
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (B) [ %s ] %s <- %s - 1 (%d)", index+1, displayLabel(), variableName, variableName, cycles));
        BaseCommand curCreator = creator;
        while (curCreator != null){
            sb.append(" <<< ").append(curCreator.toString());
            curCreator = curCreator.creator;
        }

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
        return List.of(new Decrease(variableName, label, realIndex.getAndIncrement(), creator));
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