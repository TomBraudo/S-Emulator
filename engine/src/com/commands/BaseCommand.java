package com.commands;

import com.program.ProgramState;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseCommand implements Serializable {
    public abstract void execute(ProgramState programState);
    @Override
    abstract public String toString();
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
    protected abstract List<String> getLabelsForCopy();
    protected abstract List<Integer> getConstantsForCopy();
    public abstract boolean isBaseCommand();
}




















