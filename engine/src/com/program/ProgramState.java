package com.program;

import com.commands.BaseCommand;
import com.commands.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
    The class is a helper for running full programs
 */
public class ProgramState {
    /*
        Assuming the building of the programState singleton object is smart,
        and every variable that is present in the program is entered into this map.
    */
    public HashMap<String, Variable> variables;
    public List<com.commands.BaseCommand> commands;
    public int currentCommandIndex;
    public HashMap<String, Integer> labelToIndex;
    public int cyclesCount;
    public boolean done;

    ProgramState(List<Integer> input, HashSet<String> presentVariables, List<com.commands.BaseCommand> commands, HashMap<String, Integer> labelToIndex) {
        variables = new HashMap<>();
        variables.put("y", new Variable("y", 0));
        for (int i = 0; i < input.size(); i++) {
            Variable v = new Variable(String.format("x%d",i+1), input.get(i));
            variables.put(v.getName(), v);
        }
        for(String variableName : presentVariables) {
            if(!variables.containsKey(variableName)) {
                variables.put(variableName, new Variable(variableName, 0));
            }
        }
        this.commands = commands;
        this.currentCommandIndex = 0;
        this.labelToIndex = labelToIndex;
        this.cyclesCount = 0;
        this.done = false;
    }
}

