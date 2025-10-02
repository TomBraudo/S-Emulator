package com.commands;

import com.program.ProgramState;
import com.program.SingleStepChanges;
import com.XMLHandlerV2.SInstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ZeroVariable extends BaseCommand {
    String variableName;
    ZeroVariable(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        cycles = 1;
    }
    @Override
    public void execute(ProgramState programState) {
        Variable v = programState.variables.get(variableName);
        SingleStepChanges.SingleVariableChange variableChange = new SingleStepChanges.SingleVariableChange(v.getName(), v.getValue(), 0);
        SingleStepChanges.IndexChange indexChange = new SingleStepChanges.IndexChange(programState.currentCommandIndex, programState.currentCommandIndex + 1);
        SingleStepChanges.CyclesChange cyclesChange = new SingleStepChanges.CyclesChange(programState.cyclesCount, programState.cyclesCount + cycles);
        v.setValue(0);
        programState.cyclesCount += cycles;
        programState.currentCommandIndex++;
        programState.singleStepChanges.push(new SingleStepChanges(variableChange, indexChange, cyclesChange));
    }

    @Override
    public String toString() {
        return toStringBase();
    }

    @Override
    protected String toStringBase() {
        return String.format("#%d (S) [ %s ] %s <- 0 (%d)", index+1, displayLabel(), variableName, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new ZeroVariable(variables.get(0), labels.get(0), index, creator);
    }

    @Override
    public List<String> getLabelsForCopy() {
        return List.of(label);
    }

    @Override
    public List<Integer> getConstantsForCopy() {
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
        String L1 = "L" + nextAvailableLabel.getAndIncrement();
        commands.add(new Neutral(variableName, label, realIndex.getAndIncrement(),this));
        commands.add(new Decrease(variableName, L1, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(variableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    public int getExpansionLevel() {
        return 1;
    }

    @Override
    public String getTargetLabel() {
        return NO_LABEL;
    }

    @Override
    public SInstruction toSInstruction() {
        SInstruction ins = new SInstruction();
        ins.setName("ZERO_VARIABLE");
        ins.setType("synthetic");
        ins.setSVariable(variableName);
        if (!label.equals(NO_LABEL)) ins.setSLabel(label);
        return ins;
    }
}
