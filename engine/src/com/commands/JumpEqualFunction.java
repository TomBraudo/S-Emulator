package com.commands;

import com.api.ProgramResult;
import com.program.Program;
import com.program.ProgramState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JumpEqualFunction extends BaseCommand{

    String variableName;
    String targetLabel;
    Program p;
    List<String> input;

    JumpEqualFunction(String variableName, String targetLabel, Program p, List<String> input, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        this.targetLabel = targetLabel;
        this.p = p;
        this.input = input;
        cycles = 6;
    }

    @Override
    public void execute(ProgramState programState) {
        List<Integer> input = new ArrayList<>();
        for (String s : this.input){
            input.add(programState.variables.get(s).getValue());
        }

        ProgramResult res = p.execute(input);

        if(programState.variables.get(variableName).getValue() == res.getResult()){
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
        sb.append(toStringBase());
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    protected String toStringBase() {
        return String.format("#%d (S) [ %s ] IF %s = %s(", index + 1, displayLabel(), variableName, p.getName()) +
                String.join(",", input) + ")" + String.format(" GOTO %s (%d)", targetLabel, cycles);
    }

    @Override
    public List<String> getPresentVariables() {
        return List.of(variableName);
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        List<BaseCommand> commands = new ArrayList<>();
        String z1 = "z" + nextAvailableVariable.getAndIncrement();
        commands.add(new Quotation(z1, p, input, label, realIndex.getAndIncrement(), this));
        commands.add(new JumpEqualVariable(variableName, z1, targetLabel, NO_LABEL, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    public int getExpansionLevel() {
        return p.getMaxExpansionLevel() + 2;
    }

    @Override
    public String getTargetLabel() {
        return targetLabel;
    }



    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        String variable = variables.get(0);
        List<String> input = new ArrayList<>(variables.subList(1, variables.size()));
        return new JumpEqualFunction(variable, labels.get(0), p, input, labels.get(1), index, creator);
    }

    @Override
    protected List<String> getLabelsForCopy() {
        return List.of(targetLabel, label);
    }

    @Override
    protected List<Integer> getConstantsForCopy() {
        return List.of();
    }
}
