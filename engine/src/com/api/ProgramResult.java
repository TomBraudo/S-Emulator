package com.api;
import java.util.*;

public class ProgramResult{
    private final int cycles;
    private int result;
    public record VariableToValue(String variable, int value){}
    private List<VariableToValue> variableToValue;
    private int debugIndex;
    private boolean isDebug;
    public ProgramResult(int cycles, HashMap<String, Integer> variables, int debugIndex, boolean isDebug){
        this.cycles = cycles;
        this.debugIndex = debugIndex;
        this.isDebug = isDebug;
        UnpackVariables(variables);
    }

    private void UnpackVariables(HashMap<String, Integer> variables){
        this.variableToValue = new ArrayList<>();
        for (String variableName: variables.keySet()){
            if (variableName.equals("y")){
                result = variables.get(variableName);
            }
            else {
                variableToValue.add(new VariableToValue(variableName, variables.get(variableName)));
            }
        }
        variableToValue.sort(Comparator
                .<VariableToValue, Character>comparing(v -> v.variable().charAt(0)) // first by prefix
                .thenComparingInt(v -> Integer.parseInt(v.variable().substring(1))) // then by number
        );
        variableToValue.addFirst(new VariableToValue("y", variables.get("y")));
    }

    public int getCycles() {
        return cycles;
    }
    public int getResult() {
        return result;
    }
    public List<VariableToValue> getVariableToValue() {
        return Collections.unmodifiableList(variableToValue);
    }
    public int getDebugIndex() {
        return debugIndex;
    }
    public boolean isDebug() {
        return isDebug;
    }

}
