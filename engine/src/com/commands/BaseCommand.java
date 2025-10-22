package com.commands;

import com.program.ProgramState;
import com.XMLHandlerV2.SInstruction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseCommand implements Serializable {
    public abstract void execute(ProgramState programState);
    @Override
    abstract public String toString();
    public String toDisplayString(){
        return toStringBase();
    }
    //abstract List<BaseCommands> extend();
    public static final String NO_LABEL = "-1";
    public static final String EXIT_LABEL = "EXIT";
    String label;
    int index;
    int cycles;
    BaseCommand creator; //nullable
    protected BaseCommand(String label, int index, BaseCommand creator) {
        verifyLabel(label);
        this.creator = creator;
        this.label = label;
        this.index = index;
    }
    public String getLabel() {
        return label;
    }
    public int getIndex(){
        return index;
    }
    public void setIndex(int index){
        this.index = index;
    }
    public BaseCommand getCreator(){
        return creator;
    }
    protected String displayLabel(){
        if(label.equals(NO_LABEL)){
            return "   ";
        }
        else if(label.length() == 2){
            return label + " ";
        }

        return label;
    }

    protected void verifyLabel(String label) {
        if (!label.equals(NO_LABEL) && !label.equals(EXIT_LABEL) && !label.matches("L[1-9]\\d?")) {
            throw new IllegalArgumentException("Invalid label format");
        }
    }

    protected void appendCreators(StringBuilder sb){
        if(creator != null){
            sb.append(" <<< ").append(creator.toStringBase());
            creator.appendCreators(sb);
        }
    }


    public abstract List<String> getPresentVariables();
    public abstract List<BaseCommand> expand(AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex);
    public abstract int getExpansionLevel();
    public abstract String getTargetLabel();
    protected abstract String toStringBase();
    public abstract BaseCommand copy(List<String> variables, List<Integer> constants, List<String> labels, int index, BaseCommand creator);
    public abstract List<String> getLabelsForCopy();
    public abstract List<Integer> getConstantsForCopy();
    public abstract boolean isBaseCommand();
    public abstract SInstruction toSInstruction();
    public List<String> getCommandHistory(){
        List<String> history = new ArrayList<>();
        history.add(toStringBase());
        return commandHistoryRec(history);
    }
    private List<String> commandHistoryRec(List<String> history){
        if(creator != null){
            history.add(creator.toStringBase());
            creator.commandHistoryRec(history);
        }
        return history;
    }
    public abstract String getArchitecture();
    
    /**
     * Returns all function names called by this command (recursively), or empty list if this command doesn't call functions.
     * Default implementation returns empty list; override in Quotation and JumpEqualFunction.
     */
    public java.util.List<String> getCalledFunctionNames() {
        return java.util.Collections.emptyList();
    }


}




















