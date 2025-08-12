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
        sb.append(String.format("#%d (S) [ %s ] %s <- %d (%d)", index+1, displayLabel(), variableName, value, cycles));
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
            return List.of(new ConstantAssignment(variableName, value, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        commands.addAll(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
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
}