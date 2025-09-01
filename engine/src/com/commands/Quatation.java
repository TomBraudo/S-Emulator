package com.commands;

import com.api.ProgramResult;
import com.program.Program;
import com.program.ProgramState;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Quatation extends BaseCommand{
    Program p;
    String variableName;
    List<String> input;

    protected Quatation(String variableName, Program p, List<String> input, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.p = p;
        this.variableName = variableName;
        this.input = input;
    }

    @Override
    public void execute(ProgramState programState) {
        List<Integer> input = new ArrayList<>();
        for (String s : this.input){
            input.add(programState.variables.get(s).getValue());
        }

        ProgramResult res = p.execute(input);
        programState.variables.get(variableName).setValue(res.getResult());
    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] %s <- (%s,", index + 1, displayLabel(), variableName, p.getName()) +
                String.join(",", input) + ")" + "(5 + depends on input...)";
    }

    @Override
    protected String toStringBase() {
        return String.format("#%d (S) [ %s ] %s <- (%s,", index + 1, displayLabel(), variableName, p.getName()) +
                String.join(",", input) + ")" + "(5 + depends on input...)";
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        String v = variables.get(0);
        List<String> input = new ArrayList<>(variables.subList(1, variables.size()));
        return new Quatation(v, p, input, labels.get(0), index, creator);
    }

    @Override
    protected List<String> getLabelsForCopy() {
        return List.of(label);
    }

    @Override
    protected List<Integer> getConstantsForCopy() {
        return List.of();
    }

    @Override
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        variables.addAll(input);
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        List<BaseCommand> commands = new ArrayList<>();
        HashMap<String, String> oldToNewVariables = getOldToNewVariables(nextAvailableVariable);
        HashMap<String, String> oldToNewLabels = getOldToNewLabels(nextAvailableLabel);
        String Lend = "L" + nextAvailableLabel.getAndIncrement();

        for(BaseCommand command : p.getCommands()){
            List<String> variables = command.getPresentVariables();
            variables.replaceAll(oldToNewVariables::get);
            List<String> labels = command.getLabelsForCopy();
            labels.replaceAll(oldToNewLabels::get);
            List<Integer> constants = command.getConstantsForCopy();
            commands.add(command.copy(variables, constants, labels, realIndex.getAndIncrement(), this));
        }

        return commands;
    }

    private HashMap<String, String> getOldToNewVariables(AtomicInteger nextAvailableVariable) {
        HashSet<String> pVariables = p.getPresentVariables();
        HashMap<String, String> oldToNewVariables = new HashMap<>();
        String y = "z" + nextAvailableVariable.getAndIncrement();
        oldToNewVariables.put("y", y);
        for(String s : pVariables){
            if(!oldToNewVariables.containsKey(s)){
                oldToNewVariables.put(s, "z" + nextAvailableVariable.getAndIncrement());
            }
        }

        return oldToNewVariables;
    }

    private HashMap<String, String> getOldToNewLabels(AtomicInteger nextAvailableLabel) {
        List<String> pLabels = p.getLabels();
        HashMap<String, String> oldToNewLabels = new HashMap<>();
        for(String s : pLabels){
            if(!s.equals(BaseCommand.EXIT_LABEL) && !s.equals(BaseCommand.NO_LABEL) && !oldToNewLabels.containsKey(s)) {
                oldToNewLabels.put(s, "L" + nextAvailableLabel.getAndIncrement());
            }
        }

        return oldToNewLabels;
    }

    @Override
    public int getExpansionLevel() {
        return p.getMaxExpansionLevel() + 1;
    }

    @Override
    public String getTargetLabel() {
        return NO_LABEL;
    }


}
