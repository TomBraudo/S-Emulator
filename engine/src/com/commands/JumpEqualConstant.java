package com.commands;

import com.program.ProgramState;
import com.program.SingleStepChanges;
import com.XMLHandlerV2.SInstruction;
import com.XMLHandlerV2.SInstructionArgument;
import com.XMLHandlerV2.SInstructionArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class JumpEqualConstant extends BaseCommand {
    String variableName;
    int value;
    String targetLabel;

    JumpEqualConstant(String variableName, int value, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
        this.variableName = variableName;
        this.value = value;
        this.targetLabel = targetLabel;
        cycles = 2;
    }

    @Override
    public void execute(ProgramState programState) {

        Variable v = programState.variables.get(variableName);
        int targetIndex;
        if (v.getValue() == value) {
            if (targetLabel.equals(EXIT_LABEL)){
                programState.done = true;
                return;
            }
            targetIndex = programState.labelToIndex.get(targetLabel);
        }
        else{
            targetIndex = programState.currentCommandIndex + 1;
        }
        SingleStepChanges.SingleVariableChange variableChange = new SingleStepChanges.SingleVariableChange("y", programState.variables.get("y").getValue(), programState.variables.get("y").getValue());
        SingleStepChanges.IndexChange indexChange = new SingleStepChanges.IndexChange(programState.currentCommandIndex, targetIndex);
        SingleStepChanges.CyclesChange cyclesChange = new SingleStepChanges.CyclesChange(programState.cyclesCount, programState.cyclesCount + cycles);
        programState.cyclesCount += cycles;
        programState.currentCommandIndex = targetIndex;
        programState.singleStepChanges.push(new SingleStepChanges(variableChange, indexChange, cyclesChange));
    }

    @Override
    public String toString() {
        return toStringBase();
    }

    @Override
    public String toStringBase() {
        return String.format("#%d (S) [ %s ] IF %s = %d GOTO %s (%d)", index+1, displayLabel(), variableName, value, targetLabel, cycles);
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        return new JumpEqualConstant(variables.get(0), constants.get(0), labels.get(0), labels.get(1), index, creator);
    }

    @Override
    public List<String> getLabelsForCopy() {
        return List.of(targetLabel, label);
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
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        String z1 = "z"+ nextAvailableVariable.getAndIncrement();
        commands.add(new Assignment(z1, variableName, label, realIndex.getAndIncrement(),this));
        for(int i = 0; i < value; i++){
            commands.add(new JumpZero(z1, L1, NO_LABEL, realIndex.getAndIncrement(),this));
            commands.add(new Decrease(z1,NO_LABEL, realIndex.getAndIncrement(), this));
        }
        commands.add(new JumpNotZero(z1, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new GotoLabel(targetLabel, NO_LABEL, realIndex.getAndIncrement(),this));
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

    @Override
    public SInstruction toSInstruction() {
        SInstruction ins = new SInstruction();
        ins.setName("JUMP_EQUAL_CONSTANT");
        ins.setType("synthetic");
        ins.setSVariable(variableName);
        if (!label.equals(NO_LABEL)) ins.setSLabel(label);
        SInstructionArguments args = new SInstructionArguments();
        SInstructionArgument a1 = new SInstructionArgument();
        a1.setName("constantValue");
        a1.setValue(Integer.toString(value));
        args.getSInstructionArgument().add(a1);
        SInstructionArgument a2 = new SInstructionArgument();
        a2.setName("JEConstantLabel");
        a2.setValue(targetLabel);
        args.getSInstructionArgument().add(a2);
        ins.setSInstructionArguments(args);
        return ins;
    }

    @Override
    public String getMinArchitecture() {
        return "III";
    }
}