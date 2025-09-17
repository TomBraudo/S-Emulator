package com.commands;

import com.program.ProgramState;
import com.program.SingleStepChanges;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class JumpZero extends BaseCommand {
    String variableName;
    String targetLabel;
    JumpZero(String variableName, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
        this.variableName = variableName;
        this.targetLabel = targetLabel;
        cycles = 2;
    }

    @Override
    public void execute(ProgramState programState) {
        Variable v = programState.variables.get(variableName);
        int targetIndex;
        if (v.getValue() == 0) {
            if (targetLabel.equals(EXIT_LABEL)){
                programState.done = true;
                return;
            }
            targetIndex = programState.labelToIndex.get(targetLabel);
        }
        else{
            targetIndex = programState.currentCommandIndex + 1;
        }
        SingleStepChanges.SingleVariableChange variableChange = new SingleStepChanges.SingleVariableChange("y", programState.variables.get("y").getValue(), programState.variables.get("y").getValue());
        SingleStepChanges.IndexChange indexChange = new SingleStepChanges.IndexChange(programState.currentCommandIndex, targetIndex);
        SingleStepChanges.CyclesChange cyclesChange = new SingleStepChanges.CyclesChange(programState.cyclesCount, programState.cyclesCount + cycles);
        programState.cyclesCount += cycles;
        programState.currentCommandIndex = targetIndex;
        programState.singleStepChanges.push(new SingleStepChanges(variableChange, indexChange, cyclesChange));
    }

    @Override
    public String toString() {
        return toStringBase();
    }

    @Override
    public String toStringBase() {
        return String.format("#%d (S) [ %s ] IF %s = 0 GOTO %s (%d)", index+1, displayLabel(), variableName, targetLabel, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new JumpZero(variables.get(0), labels.get(0), labels.get(1), index, creator);
    }

    @Override
    protected List<String> getLabelsForCopy() {
        return List.of(targetLabel, label);
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
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        commands.add(new JumpNotZero(variableName, L1, label, realIndex.getAndIncrement(), this));
        commands.add(new GotoLabel(targetLabel, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Neutral(variableName, L1, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    public int getExpansionLevel() {
        return 2;
    }

    @Override
    public String getTargetLabel() {
        return targetLabel;
    }
}