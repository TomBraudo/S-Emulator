package program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import XMLHandler.*;

public class Program {
    String name;
    List<BaseCommand> commands;
    HashSet<String> presentVariables;
    HashMap<String, Integer> labelToIndex;
    List<String> inputVariables;
    List<String> labels;

    Program(String name, List<BaseCommand> commands){
        this.name = name;
        this.commands = commands;
        unpackCommands();
    }

    /*
        The method receives input for the program, runs it, and return the programState.
        Result at programState.variables.get("y")
        Cycles count at programState.cyclesCount
     */
    ProgramResult execute(List<Integer> input){
        ProgramState programState = new ProgramState(input, presentVariables, commands, labelToIndex);
        while (!programState.done && programState.currentCommandIndex < commands.size()){
            BaseCommand command = commands.get(programState.currentCommandIndex);
            command.execute(programState);
        }
        return new ProgramResult(programState.cyclesCount, programState.variables);
    }

    void unpackCommands(){
        inputVariables = new ArrayList<>();
        presentVariables = new HashSet<>();
        labelToIndex = new HashMap<>();
        labels = new ArrayList<>();
        for(int i = 0; i < commands.size(); i++){
            BaseCommand command = commands.get(i);
            if(!command.getLabel().equals(BaseCommand.NO_LABEL)){
                //The only label that matters is the first of its kind in the program.
                if(!labelToIndex.containsKey(command.getLabel())){
                    labelToIndex.put(command.getLabel(), i);
                }
                labels.add(command.getLabel());
            }
            HashSet<String> commandVariables = command.getPresentVariables();
            presentVariables.addAll(commandVariables);
            for(String variable : commandVariables){
                if(variable.startsWith("x") && !inputVariables.contains(variable)){
                    inputVariables.add(variable);
                }
            }
        }
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
        for(BaseCommand command : commands){
            sb.append(command.toString()).append("\n");
        }

        return sb.toString();
    }

}
