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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] GOTO %s (%d)", index+1, displayLabel(), targetLabel, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    public HashSet<String> getPresentVariables() {
        return new HashSet<>();
    }

    @Override
    public List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if(level == 0){
            return List.of(new GotoLabel(targetLabel, label, realIndex.getAndIncrement(), creator));
        }
        String nextVar = "z" + nextAvailableLabel.getAndIncrement();
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
