package com.dto.api;
import java.util.*;

public class ProgramResult{
    private final int cycles;
    private final int sessionCycles;
    private int result;
    public record VariableToValue(String variable, int value){}
    private List<VariableToValue> variableToValue;
    private int debugIndex;
    private boolean isDebug;
    public enum HaltReason { FINISHED, STOPPED_MANUALLY, INSUFFICIENT_CREDITS }
    private final HaltReason haltReason;
    public ProgramResult(int cycles, HashMap<String, Integer> variables, int debugIndex, boolean isDebug){
        this.cycles = cycles;
        this.sessionCycles = cycles; // For non-debug, session cycles equals total cycles
        this.debugIndex = debugIndex;
        this.isDebug = isDebug;
        this.haltReason = isDebug ? HaltReason.STOPPED_MANUALLY : HaltReason.FINISHED;
        UnpackVariables(variables);
    }

    public ProgramResult(int cycles, HashMap<String, Integer> variables, int debugIndex, boolean isDebug, HaltReason haltReason){
        this.cycles = cycles;
        this.sessionCycles = cycles; // For non-debug, session cycles equals total cycles
        this.debugIndex = debugIndex;
        this.isDebug = isDebug;
        this.haltReason = haltReason;
        UnpackVariables(variables);
    }

    public ProgramResult(int cycles, int sessionCycles, HashMap<String, Integer> variables, int debugIndex, boolean isDebug, HaltReason haltReason){
        this.cycles = cycles;
        this.sessionCycles = sessionCycles;
        this.debugIndex = debugIndex;
        this.isDebug = isDebug;
        this.haltReason = haltReason;
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
                .<VariableToValue, Character>comparing(v -> v.variable().charAt(0))
                .thenComparingInt(v -> Integer.parseInt(v.variable().substring(1)))
        );
        variableToValue.addFirst(new VariableToValue("y", variables.get("y")));
    }

    public int getCycles() {
        return cycles;
    }
    public int getSessionCycles() {
        return sessionCycles;
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

    public HaltReason getHaltReason(){
        return haltReason;
    }

}


