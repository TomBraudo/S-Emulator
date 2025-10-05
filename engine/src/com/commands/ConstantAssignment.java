package com.commands;

import com.program.ProgramState;
import com.program.SingleStepChanges;
import com.XMLHandlerV2.SInstruction;
import com.XMLHandlerV2.SInstructionArgument;
import com.XMLHandlerV2.SInstructionArguments;

import java.util.ArrayList;
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
        Variable v = programState.variables.get(variableName);
        SingleStepChanges.SingleVariableChange variableChange = new SingleStepChanges.SingleVariableChange(v.getName(), v.getValue(), value);
        SingleStepChanges.IndexChange indexChange = new SingleStepChanges.IndexChange(programState.currentCommandIndex, programState.currentCommandIndex + 1);
        SingleStepChanges.CyclesChange cyclesChange = new SingleStepChanges.CyclesChange(programState.cyclesCount, programState.cyclesCount + cycles);
        programState.cyclesCount += cycles;
        v.setValue(value);
        programState.currentCommandIndex++;
        programState.singleStepChanges.push(new SingleStepChanges(variableChange, indexChange, cyclesChange));
    }

    @Override
    public String toString() {
        return toStringBase();
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
        commands.add(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), this));
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

    @Override
    protected String toStringBase() {
        return String.format("#%d (S) [ %s ] %s <- %d (%d)", index+1, displayLabel(), variableName, value, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new ConstantAssignment(variables.get(0), constants.get(0), labels.get(0), index, creator);
    }

    @Override
    public List<String> getLabelsForCopy() {
        return List.of(label);
    }

    @Override
    public List<Integer> getConstantsForCopy() {
        return List.of(value);
    }

    @Override
    public boolean isBaseCommand() {
        return false;
    }

    @Override
    public SInstruction toSInstruction() {
        SInstruction ins = new SInstruction();
        ins.setName("CONSTANT_ASSIGNMENT");
        ins.setType("synthetic");
        ins.setSVariable(variableName);
        if (!label.equals(NO_LABEL)) ins.setSLabel(label);
        SInstructionArguments args = new SInstructionArguments();
        SInstructionArgument arg = new SInstructionArgument();
        arg.setName("constantValue");
        arg.setValue(Integer.toString(value));
        args.getSInstructionArgument().add(arg);
        ins.setSInstructionArguments(args);
        return ins;
    }

    @Override
    public String getMinArchitecture() {
        return "II";
    }
}