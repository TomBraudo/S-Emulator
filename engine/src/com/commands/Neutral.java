package com.commands;

import com.program.ProgramState;

import java.util.ArrayList;
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
        sb.append(toStringBase());
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    public String toStringBase() {
        return String.format("#%d (B) [ %s ] %s <- %s (%d)", index+1, displayLabel(), variableName, variableName, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new Neutral(variables.get(0), labels.get(0), index, creator);
    }

    @Override
    protected List<String> getLabelsForCopy() {
        return List.of(label);
    }

    @Override
    protected List<Integer> getConstantsForCopy() {
        return List.of();
    }

    @Override
    public boolean isBaseCommand() {
        return true;
    }

    @Override
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
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