package com.commands;

import com.program.ProgramState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ZeroVariable extends BaseCommand {
    String variableName;
    ZeroVariable(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        cycles = 1;
    }
    @Override
    public void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        v.setValue(0);
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        return toStringBase();
    }

    @Override
    protected String toStringBase() {
        return String.format("#%d (S) [ %s ] %s <- 0 (%d)", index+1, displayLabel(), variableName, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new ZeroVariable(variables.get(0), labels.get(0), index, creator);
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
        return false;
    }

    @Override
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L" + nextAvailableLabel.getAndIncrement();
        commands.add(new Neutral(variableName, label, realIndex.getAndIncrement(),this));
        commands.add(new Decrease(variableName, L1, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(variableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    public int getExpansionLevel() {
        return 1;
    }

    @Override
    public String getTargetLabel() {
        return NO_LABEL;
    }
}
