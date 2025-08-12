package com.commands;

import com.program.ProgramState;

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
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        if (v.getValue() == 0) {
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
        sb.append(String.format("#%d (S) [ %s ] IF %s = 0 GOTO %s (%d)", index+1, displayLabel(), variableName, targetLabel, cycles));
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
        if(level == 0){
            return List.of(new JumpZero(variableName, targetLabel, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        commands.add(new JumpNotZero(variableName, L1, label, realIndex.getAndIncrement(), this));
        commands.addAll(new GotoLabel(targetLabel, NO_LABEL, realIndex.getAndIncrement(), this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
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