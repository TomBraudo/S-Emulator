package com.api;

import com.commands.Variable;

import java.util.*;

public class ProgramResult{
    private final int cycles;
    private int result;
    private List<AbstractMap.SimpleEntry<String,Integer>> variableToValue;
    public ProgramResult(int cycles, HashMap<String, Variable> variables){
        this.cycles = cycles;
        UnpackVariables(variables);

    }

    private void UnpackVariables(HashMap<String, Variable> variables){
        this.variableToValue = new ArrayList<>();
        for (String variableName: variables.keySet()){
            if (variableName.equals("y")){
                result = variables.get(variableName).getValue();
            }
            else {
                variableToValue.add(new AbstractMap.SimpleEntry<>(variableName, variables.get(variableName).getValue()));
            }

            variableToValue.sort(Comparator.comparing(AbstractMap.SimpleEntry::getKey));
        }
    }

    public int getCycles() {
        return cycles;
    }
    public int getResult() {
        return result;
    }
    public List<AbstractMap.SimpleEntry<String,Integer>> getVariableToValue() {
        return variableToValue;
    }
}
