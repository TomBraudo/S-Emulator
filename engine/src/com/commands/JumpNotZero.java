package com.commands;

import com.program.ProgramState;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class JumpNotZero extends BaseCommand {
    String variableName;
    String targetLabel;
    JumpNotZero(String variableName, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        this.targetLabel = targetLabel;
        verifyLabel(targetLabel);
        cycles = 2;
    }
    @Override
    public void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        if (v.getValue() > 0) {
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
        sb.append(String.format("#%d (B) [ %s ] IF %s != 0 GOTO %s (%d)", index+1, displayLabel(), variableName, targetLabel, cycles));
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
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        return List.of(new JumpNotZero(variableName, targetLabel, label, realIndex.getAndIncrement(), creator));
    }

    @Override
    public int getExpansionLevel() {
        return 0;
    }

    @Override
    public String getTargetLabel() {
        return targetLabel;
    }
}