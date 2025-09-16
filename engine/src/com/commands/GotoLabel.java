package com.commands;

import com.program.ProgramState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class GotoLabel extends BaseCommand {
    String targetLabel;
    GotoLabel(String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
        this.targetLabel = targetLabel;
        cycles = 1;
    }
    @Override
    public void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        if (targetLabel.equals(EXIT_LABEL)){
            programState.done = true;
            return;
        }
        programState.currentCommandIndex = programState.labelToIndex.get(targetLabel);
    }

    @Override
    public String toString() {
        return toStringBase();
    }

    @Override
    public String toStringBase() {
        return String.format("#%d (S) [ %s ] GOTO %s (%d)", index+1, displayLabel(), targetLabel, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new GotoLabel(labels.get(0), labels.get(1), index, creator);
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
        return new ArrayList<>();
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        String nextVar = "z" + nextAvailableVariable.getAndIncrement();
        List<BaseCommand> commands = new ArrayList<>();
        commands.add(new Increase(nextVar, label, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(nextVar, targetLabel, NO_LABEL, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    public int getExpansionLevel() {
        return 1;
    }

    @Override
    public String getTargetLabel() {
        return targetLabel;
    }


}
