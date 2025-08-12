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
        sb.append(String.format("#%d (S) [ %s ] IF %s = %s GOTO %s (%d)", index+1, displayLabel(), variableName, otherVariableName, targetLabel, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    public HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        variables.add(otherVariableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if (level == 0){
            return List.of(new JumpEqualVariable(variableName, otherVariableName, targetLabel, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        String L2 = "L"+ nextAvailableLabel.getAndIncrement();
        String L3 = "L"+ nextAvailableLabel.getAndIncrement();
        String z1 = "Z"+ nextAvailableVariable.getAndIncrement();
        String z2 = "Z"+ nextAvailableVariable.getAndIncrement();

        commands.addAll(new Assignment(z1, variableName, label, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new Assignment(z2, otherVariableName, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new JumpZero(z1, L3, L2, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new JumpZero(z2, L1, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new Decrease(z1, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.add(new Decrease(z2, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.addAll(new GotoLabel(L2, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new JumpZero(z2, targetLabel, L3, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
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