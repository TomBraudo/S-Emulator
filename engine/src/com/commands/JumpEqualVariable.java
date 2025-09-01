package com.commands;

import com.program.ProgramState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class JumpEqualVariable extends BaseCommand {
    String variableName;
    String otherVariableName;
    String targetLabel;
    JumpEqualVariable(String variableName, String otherVariableName, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
        this.variableName = variableName;
        this.otherVariableName = otherVariableName;
        this.targetLabel = targetLabel;
        cycles = 2;
    }

    @Override
    public void execute(ProgramState programState) {
        programState.cyclesCount += 2;
        Variable v1 = programState.variables.get(variableName);
        Variable v2 = programState.variables.get(otherVariableName);
        if (v1.getValue() == v2.getValue()) {
            if (targetLabel.equals(EXIT_LABEL)){
                programState.done = true;
                return;
            }
            programState.currentCommandIndex = programState.labelToIndex.get(targetLabel);
        }
        else{
            programState.currentCommandIndex++;
        }
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
        return String.format("#%d (S) [ %s ] IF %s = %s GOTO %s (%d)", index+1, displayLabel(), variableName, otherVariableName, targetLabel, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new JumpEqualVariable(variables.get(0), variables.get(1), labels.get(0), labels.get(1), index, creator);
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
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        variables.add(otherVariableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        String L2 = "L"+ nextAvailableLabel.getAndIncrement();
        String L3 = "L"+ nextAvailableLabel.getAndIncrement();
        String z1 = "z"+ nextAvailableVariable.getAndIncrement();
        String z2 = "z"+ nextAvailableVariable.getAndIncrement();

        commands.add(new Assignment(z1, variableName, label, realIndex.getAndIncrement(),this));
        commands.add(new Assignment(z2, otherVariableName, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.add(new JumpZero(z1, L3, L2, realIndex.getAndIncrement(),this));
        commands.add(new JumpZero(z2, L1, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.add(new Decrease(z1, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.add(new Decrease(z2, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.add(new GotoLabel(L2, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.add(new JumpZero(z2, targetLabel, L3, realIndex.getAndIncrement(),this));
        commands.add(new Neutral(variableName, L1, realIndex.getAndIncrement(),this));
        return commands;
    }

    @Override
    public int getExpansionLevel() {
        return 3;
    }

    @Override
    public String getTargetLabel() {
        return targetLabel;
    }
}