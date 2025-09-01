package com.program;

import com.XMLHandlerV2.SInstruction;
import com.api.ProgramResult;
import com.commands.BaseCommand;
import com.commands.CommandFactory;
import com.commands.Variable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Program implements Serializable {
    String name;
    List<com.commands.BaseCommand> commands;
    HashSet<String> presentVariables;
    HashMap<String, Integer> labelToIndex;
    List<String> inputVariables;
    List<String> labels;

    public Program(String name, List<com.commands.BaseCommand> commands){
        this.name = name;
        this.commands = commands;
        unpackCommands();
    }

    /*
        The method receives input for the program, runs it, and return the programState.
        Result at programState.variables.get("y")
        Cycles count at programState.cyclesCount
     */
    public String getName() {return name;}
    public ProgramResult execute(List<Integer> input){
        ProgramState programState = new ProgramState(input, presentVariables, commands, labelToIndex);
        while (!programState.done && programState.currentCommandIndex < commands.size()){
            com.commands.BaseCommand command = commands.get(programState.currentCommandIndex);
            command.execute(programState);
        }
        return new ProgramResult(programState.cyclesCount, variableToValue(programState));
    }

    private int getMaxWorkVariable(){
        int max = 0;
        for (String variables : presentVariables) {
            if (variables.charAt(0) == 'z'){
                max = Math.max(max, Integer.parseInt(variables.substring(1)));
            }
        }

        return max;
    }
    private int getMaxLabel(){
        int max = 0;
        for (String label : labels) {
            if (label.charAt(0) == 'L'){
                max = Math.max(max, Integer.parseInt(label.substring(1)));
            }
        }
        return max;
    }

    void unpackCommands(){
        inputVariables = new ArrayList<>();
        presentVariables = new HashSet<>();
        labelToIndex = new HashMap<>();
        labels = new ArrayList<>();
        for(int i = 0; i < commands.size(); i++){
            com.commands.BaseCommand command = commands.get(i);
            if(!command.getLabel().equals(com.commands.BaseCommand.NO_LABEL)){
                //The only label that matters is the first of its kind in the program.
                if(!labelToIndex.containsKey(command.getLabel())){
                    labelToIndex.put(command.getLabel(), i);
                }
                labels.add(command.getLabel());
            }
            List<String> commandVariables = command.getPresentVariables();
            presentVariables.addAll(commandVariables);
            for(String variable : commandVariables){
                if(variable.startsWith("x") && !inputVariables.contains(variable)){
                    inputVariables.add(variable);
                }
            }
        }
    }

    public Program expand(int level){
        List<BaseCommand> currentCommands = commands;
        AtomicInteger nextAvailableLabel = new AtomicInteger(getMaxLabel()+1);
        AtomicInteger nextAvailableVariable = new AtomicInteger(getMaxWorkVariable()+1);
        AtomicInteger realIndex = new AtomicInteger(0);
        for(int i = 0; i < level; i++){
            List<BaseCommand> newCommands = new ArrayList<>();
            realIndex.set(0);
            for(BaseCommand command : currentCommands){
                newCommands.addAll(command.expand(nextAvailableVariable, nextAvailableLabel, realIndex));
            }
            currentCommands = newCommands;
        }

        return new Program(name, currentCommands);
    }

    public int getMaxExpansionLevel(){
        return commands.stream()
                .mapToInt(com.commands.BaseCommand::getExpansionLevel)
                .max()
                .orElse(0);
    }

    public void verifyLegal(){
        for(com.commands.BaseCommand command : commands){
            String targetLabel = command.getTargetLabel();
            if(!targetLabel.equals(com.commands.BaseCommand.NO_LABEL) && !targetLabel.equals(com.commands.BaseCommand.EXIT_LABEL) && !labelToIndex.containsKey(targetLabel)){
                throw new IllegalArgumentException("Target Label is not in list");
            }
        }
    }

    private HashMap<String,Integer> variableToValue(ProgramState state){
        return (HashMap<String, Integer>) state.variables.values().stream()
                .collect(Collectors.toMap(Variable::getName, Variable::getValue));
    }

    public List<String> getInputVariables() {
        return Collections.unmodifiableList(inputVariables);
    }


    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("Input variables used in the program (in order): ");
        for (String inputVariable : inputVariables){
            sb.append(inputVariable).append(", ");
        }

        //Remove the last ", "
        if(!inputVariables.isEmpty()){
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("\n");
        sb.append("The labels used in the program (in order): ");
        for(String label : labels){
            sb.append(label).append(", ");
        }

        //Remove the last ", "
        if(!labels.isEmpty()){
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("\n");
        sb.append("The program: \n");
        for(com.commands.BaseCommand command : commands){
            sb.append(command.toString()).append("\n");
        }

        return sb.toString();
    }

    public HashSet<String> getPresentVariables() {
        return (HashSet<String>) Collections.unmodifiableCollection(presentVariables);
    }

    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    public List<BaseCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public static Program createProgram(String name, List<SInstruction> instructions) {
        List<BaseCommand> commands = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            SInstruction ins = instructions.get(i);

            commands.add(
                    CommandFactory.createCommand(
                            ins.getName(),
                            ins.getSVariable(),
                            ins.getSLabel(),
                            ins.getSInstructionArguments() == null
                                    ? null
                                    : ins.getSInstructionArguments().getSInstructionArgument(),
                            i
                    )
            );
        }

        Program program = new Program(name, commands);
        program.verifyLegal();
        return program;
    }


}
