package com.commands;

import com.program.ProgramState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        programState.cyclesCount += cycles;
        Variable v1 = programState.variables.get(variableName);
        Variable v2 = programState.variables.get(otherVariableName);
        v1.setValue(v2.getValue());
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] %s <- %s (%d)", index + 1, displayLabel(), variableName, otherVariableName, cycles));
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
        if (level == 0) {
            return List.of(new Assignment(variableName, otherVariableName, label, realIndex.getAndIncrement(), creator));
        }
        String L1 = "L" + nextAvailableLabel.getAndIncrement();
        String L2 = "L" + nextAvailableLabel.getAndIncrement();
        String L3 = "L" + nextAvailableLabel.getAndIncrement();
        String z1 = "z" + nextAvailableLabel.getAndIncrement();
        List<BaseCommand> commands = new ArrayList<>();
        commands.addAll(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), this).expand(level - 1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new JumpNotZero(otherVariableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.addAll(new GotoLabel(L3, NO_LABEL, realIndex.getAndIncrement(), this).expand(level - 1, nextAvailableVariable, nextAvailableLabel, realIndex));
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
}