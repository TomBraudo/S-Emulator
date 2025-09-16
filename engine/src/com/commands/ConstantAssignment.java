package com.commands;

import com.program.ProgramState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ConstantAssignment extends BaseCommand {
    String variableName;
    int value;
    ConstantAssignment(String variableName, int value, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        this.value = value;
        cycles = 2;
    }

    @Override
    public void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        v.setValue(value);
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
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {

        List<BaseCommand> commands = new ArrayList<>();
        commands.add(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), this));
        for(int i = 0; i < value; i++){
            commands.add(new Increase(variableName, NO_LABEL, realIndex.getAndIncrement(), this));
        }
        return commands;
    }

    @Override
    public int getExpansionLevel() {
        return 2;
    }

    @Override
    public String getTargetLabel() {
        return NO_LABEL;
    }

    @Override
    protected String toStringBase() {
        return String.format("#%d (S) [ %s ] %s <- %d (%d)", index+1, displayLabel(), variableName, value, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new ConstantAssignment(variables.get(0), constants.get(0), labels.get(0), index, creator);
    }

    @Override
    protected List<String> getLabelsForCopy() {
        return List.of(label);
    }

    @Override
    protected List<Integer> getConstantsForCopy() {
        return List.of(value);
    }

    @Override
    public boolean isBaseCommand() {
        return false;
    }
}