package com.commands;

import com.program.ProgramState;
import com.program.SingleStepChanges;
import com.XMLHandlerV2.SInstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Neutral extends BaseCommand {
    String variableName;
    Neutral(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        cycles = 0;
    }
    @Override
    public void execute(ProgramState programState) {
        SingleStepChanges.SingleVariableChange variableChange = new SingleStepChanges.SingleVariableChange(variableName, programState.variables.get(variableName).getValue(), programState.variables.get(variableName).getValue());
        SingleStepChanges.IndexChange indexChange = new SingleStepChanges.IndexChange(programState.currentCommandIndex, programState.currentCommandIndex + 1);
        SingleStepChanges.CyclesChange cyclesChange = new SingleStepChanges.CyclesChange(programState.cyclesCount, programState.cyclesCount + cycles);
        programState.cyclesCount += cycles;
        programState.currentCommandIndex++;
        programState.singleStepChanges.push(new SingleStepChanges(variableChange, indexChange, cyclesChange));
    }

    @Override
    public String toString() {
        return toStringBase();
    }

    @Override
    public String toStringBase() {
        return String.format("#%d (B) [ %s ] %s <- %s (%d) | %s", index+1, displayLabel(), variableName, variableName, cycles, getArchitecture());
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new Neutral(variables.get(0), labels.get(0), index, creator);
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
        return true;
    }

    @Override
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        return List.of(new Neutral(variableName, label, realIndex.getAndIncrement(), creator));
    }

    @Override
    public int getExpansionLevel() {
        return 0;
    }

    @Override
    public String getTargetLabel() {
        return NO_LABEL;
    }

    @Override
    public SInstruction toSInstruction() {
        SInstruction ins = new SInstruction();
        ins.setName("NEUTRAL");
        ins.setType("basic");
        ins.setSVariable(variableName);
        if (!label.equals(NO_LABEL)) ins.setSLabel(label);
        return ins;
    }

    @Override
    public String getArchitecture() {
        return "I";
    }
}