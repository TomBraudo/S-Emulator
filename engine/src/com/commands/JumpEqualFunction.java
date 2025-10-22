package com.commands;

import com.XMLHandlerV2.SInstruction;
import com.dto.api.ProgramResult;
import com.program.Program;
import com.program.ProgramState;
import com.program.SingleStepChanges;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class JumpEqualFunction extends BaseCommand{

    String variableName;
    String targetLabel;
    Program p;
    // Each element is either String (variable name) or ArgExpr.ArgCall (nested function call)
    List<Object> input;

    // Unified constructor: accepts either List<String> or List<ArgExpr.ArgCall> or a mixed List<?>
    JumpEqualFunction(String variableName, String targetLabel, Program p, List<?> input, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        this.targetLabel = targetLabel;
        this.p = p;
        // Normalize to List<Object> while validating element types
        List<Object> normalized = new ArrayList<>(input == null ? 0 : input.size());
        if (input != null) {
            for (Object o : input) {
                if (o instanceof String || o instanceof ArgExpr.ArgCall) {
                    normalized.add(o);
                } else {
                    throw new IllegalArgumentException("Unsupported argument element type: " + o);
                }
            }
        }
        this.input = normalized;
        cycles = 6;
    }

    @Override
    public void execute(ProgramState programState) {
        List<Integer> evaluated = FnArgs.evaluateArgs(programState, input);
        ProgramResult res = p.executeWithBudget(evaluated, Integer.MAX_VALUE);
        int targetIndex;
        if(programState.variables.get(variableName).getValue() == res.getResult()){
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
    protected String toStringBase() {
        List<String> parts = FnArgs.renderArgList(input);
        return String.format("#%d (S) [ %s ] IF %s = %s(", index + 1, displayLabel(), variableName, p.getName()) +
                String.join(",", parts) + ")" + String.format(" GOTO %s (X + %d) | %s", targetLabel, cycles, getArchitecture());
    }

    @Override
    public List<String> getPresentVariables() {
        List<String> vars = new ArrayList<>();
        vars.add(variableName);
        vars.addAll(FnArgs.collectVariables(input));
        return vars;
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
        // This command expands in one step into:
        //   Quotation(z1, p, input, ...), JumpEqualVariable(variableName, z1, ...)
        // The JumpEqualVariable has fixed expansion depth 3.
        // The Quotation depth depends on the quoted program and nested argument calls.
        // Therefore overall depth = 1 + max( quotationDepth(input, p), 3 ).
        int quotationDepth = 1 + Math.max(p.getMaxExpansionLevel(), ArgExpr.computeArgsDepth(input));
        return 1 + Math.max(quotationDepth, 3);
    }

    @Override
    public String getTargetLabel() {
        return targetLabel;
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        String variable = variables.get(0);
        Deque<String> mapped = new ArrayDeque<>(variables.subList(1, variables.size()));
        List<Object> newInput = FnArgs.replaceVarsInArgs(this.input, mapped);
        return new JumpEqualFunction(variable, labels.get(0), p, newInput, labels.get(1), index, creator);
    }

    @Override
    public List<String> getLabelsForCopy() {
        return List.of(targetLabel, label);
    }

    @Override
    public List<Integer> getConstantsForCopy() {
        return List.of();
    }

    @Override
    public boolean isBaseCommand() {
        return false;
    }

    //Not supported
    @Override
    public SInstruction toSInstruction() {
        return null;
    }

    @Override
    public String getArchitecture() {
        return "IV";
    }
    
    @Override
    public String getCalledFunctionName() {
        return p != null ? p.getName() : null;
    }
}
