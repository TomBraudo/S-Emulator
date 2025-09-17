package com.commands;

import com.program.ProgramState;
import com.program.SingleStepChanges;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

class Assignment extends BaseCommand {
    String variableName;
    String otherVariableName;

    Assignment(String variableName, String otherVariableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        this.otherVariableName = otherVariableName;
        cycles = 4;
    }

    @Override
    public void execute(ProgramState programState) {
        Variable v1 = programState.variables.get(variableName);
        Variable v2 = programState.variables.get(otherVariableName);
        SingleStepChanges.SingleVariableChange variableChange = new SingleStepChanges.SingleVariableChange(v1.getName(), v1.getValue(), v2.getValue());
        SingleStepChanges.IndexChange indexChange = new SingleStepChanges.IndexChange(programState.currentCommandIndex, programState.currentCommandIndex+ 1);
        SingleStepChanges.CyclesChange cyclesChange = new SingleStepChanges.CyclesChange(programState.cyclesCount, programState.cyclesCount + cycles);
        programState.cyclesCount += cycles;
        v1.setValue(v2.getValue());
        programState.currentCommandIndex++;
        programState.singleStepChanges.push(new SingleStepChanges(variableChange, indexChange, cyclesChange));
    }

    @Override
    public String toString() {
        return toStringBase();
    }



    @Override
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        variables.add(otherVariableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        String L1 = "L" + nextAvailableLabel.getAndIncrement();
        String L2 = "L" + nextAvailableLabel.getAndIncrement();
        String L3 = "L" + nextAvailableLabel.getAndIncrement();
        String z1 = "z" + nextAvailableVariable.getAndIncrement();
        List<BaseCommand> commands = new ArrayList<>();
        commands.add(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(otherVariableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new GotoLabel(L3, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Decrease(otherVariableName, L1, realIndex.getAndIncrement(), this));
        commands.add(new Increase(z1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(otherVariableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Decrease(z1, L2, realIndex.getAndIncrement(), this));
        commands.add(new Increase(variableName, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Increase(otherVariableName, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(z1, L2, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Neutral(variableName, L3, realIndex.getAndIncrement(), this));
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
        return String.format("#%d (S) [ %s ] %s <- %s (%d)", index + 1, displayLabel(), variableName, otherVariableName, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new Assignment(variables.get(0), variables.get(1), labels.get(0), index, creator);
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
}