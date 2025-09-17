package com.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class SingleStepChanges {
    public record SingleVariableChange(String variable, int oldValue, int newValue) {}
    public record IndexChange(int oldValue, int newValue){}
    public record CyclesChange(int oldValue, int newValue){}

    private final SingleVariableChange variableChanges;
    private final IndexChange indexChange;
    private final CyclesChange cyclesChange;
    public SingleStepChanges(SingleVariableChange variableChanges, IndexChange indexChange, CyclesChange cyclesChange){
        this.variableChanges = variableChanges;
        this.indexChange = indexChange;
        this.cyclesChange = cyclesChange;
    }

    public SingleVariableChange getVariableChanges() {
        return variableChanges;
    }
    public IndexChange getIndexChange() {
        return indexChange;
    }
    public CyclesChange getCyclesChange() {
        return cyclesChange;
    }
}
