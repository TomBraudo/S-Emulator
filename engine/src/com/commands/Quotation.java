package com.commands;

import com.XMLHandlerV2.SInstruction;
import com.api.ProgramResult;
import com.program.Program;
import com.program.ProgramState;
import com.program.SingleStepChanges;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class Quotation extends BaseCommand{
    Program p;
    String variableName;
    // Each element is either String (variable name) or ArgExpr.ArgCall (nested function call)
    List<Object> input;

    protected Quotation(String variableName, Program p, List<?> input, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.p = p;
        this.variableName = variableName;
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
    }

    @Override
    public void execute(ProgramState programState) {
        List<Integer> evaluated = FnArgs.evaluateArgs(programState, input);
        ProgramResult res = p.execute(evaluated);
        SingleStepChanges.SingleVariableChange variableChange = new SingleStepChanges.SingleVariableChange(variableName, programState.variables.get(variableName).getValue(), res.getResult());
        SingleStepChanges.IndexChange indexChange = new SingleStepChanges.IndexChange(programState.currentCommandIndex, programState.currentCommandIndex + 1);
        SingleStepChanges.CyclesChange cyclesChange = new SingleStepChanges.CyclesChange(programState.cyclesCount, programState.cyclesCount + res.getCycles() + 5);
        programState.variables.get(variableName).setValue(res.getResult());
        programState.cyclesCount += res.getCycles() + 5;
        programState.currentCommandIndex++;
        programState.singleStepChanges.push(new SingleStepChanges(variableChange, indexChange, cyclesChange));
    }

    @Override
    public String toString() {
        return toStringBase();
    }

    @Override
    protected String toStringBase() {
        List<String> parts = FnArgs.renderArgList(input);
        return String.format("#%d (S) [ %s ] %s <- (%s,", index + 1, displayLabel(), variableName, p.getName()) +
                String.join(",", parts) + ")" + "(5 + depends on input...)";
    }

    @Override
    public BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator) {
        String v = variables.get(0);
        Deque<String> mapped = new ArrayDeque<>(variables.subList(1, variables.size()));
        List<Object> newInput = FnArgs.replaceVarsInArgs(this.input, mapped);
        return new Quotation(v, p, newInput, labels.get(0), index, creator);
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
    public boolean isBaseCommand() {
        return false;
    }

    //Not supported
    @Override
    public SInstruction toSInstruction() {
        return null;
    }

    @Override
    public List<String> getPresentVariables() {
        List<String> variables = new ArrayList<>();
        variables.add(variableName);
        variables.addAll(FnArgs.collectVariables(input));
        return variables;
    }

    @Override
    public List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        List<BaseCommand> commands = new ArrayList<>();
        HashMap<String, String> oldToNewVariables = getOldToNewVariables(nextAvailableVariable);
        HashMap<String, String> oldToNewLabels = getOldToNewLabels(nextAvailableLabel);
        String lEnd = "L" + nextAvailableLabel.getAndIncrement();
        oldToNewLabels.put(BaseCommand.EXIT_LABEL, lEnd);

        commands.add(new Neutral("y", label, realIndex.getAndIncrement(), this));

        // 1) Compute preamble: for each input arg, prepare the corresponding fresh inner input variable
        List<String> innerInputs = p.getInputVariables();
        for (int i = 0; i < input.size(); i++) {
            String innerVar = innerInputs.get(i);                    // e.g., x1, x2
            String mappedInner = oldToNewVariables.get(innerVar);    // fresh zK for inner x_i
            Object arg = input.get(i);
            if (arg instanceof String varName) {
                commands.add(new Assignment(mappedInner, varName, BaseCommand.NO_LABEL, realIndex.getAndIncrement(), this));
            }
            else {
                ArgExpr.ArgCall call = (ArgExpr.ArgCall) arg;
                Program nested = FnArgs.getProgramByName(call.name());
                commands.add(new Quotation(mappedInner, nested, call.args(), BaseCommand.NO_LABEL, realIndex.getAndIncrement(), this));
            }

        }

        // 2) Inline the quoted program with variable and label remapping
        for (BaseCommand command : p.getCommands()){
            List<String> variables = new ArrayList<>(command.getPresentVariables());
            variables.replaceAll(oldToNewVariables::get);
            List<String> labels = new ArrayList<>(command.getLabelsForCopy());
            labels.replaceAll(oldToNewLabels::get);
            List<Integer> constants = command.getConstantsForCopy();
            commands.add(command.copy(variables, constants, labels, realIndex.getAndIncrement(), this));
        }

        // 3) Move inner result to destination variable
        commands.add(new Assignment(variableName, oldToNewVariables.get("y"), lEnd, realIndex.getAndIncrement(), this));

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
            if(!s.equals(BaseCommand.EXIT_LABEL) && !oldToNewLabels.containsKey(s)) {
                oldToNewLabels.put(s, "L" + nextAvailableLabel.getAndIncrement());
            }
        }
        oldToNewLabels.put(BaseCommand.NO_LABEL, BaseCommand.NO_LABEL);

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
