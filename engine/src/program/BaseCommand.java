package program;

import java.util.HashSet;

abstract class BaseCommand {
    abstract void execute(ProgramState programState);
    @Override
    abstract public String toString();
    //abstract List<BaseCommands> extend();
    static final String NO_LABEL = "-1";
    static final String EXIT_LABEL = "Exit";
    String label;
    int index;
    int cycles;
    protected BaseCommand(String label, int index) {
        this.label = label;
        this.index = index;
    }
    String getLabel() {
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


    abstract HashSet<String>  getPresentVariables();
}

class Increase extends BaseCommand {
    String variableName;
    Increase(String variableName, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        cycles = 1;
    }

    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        v.setValue(v.getValue() + 1);
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {

        return String.format("#%d (B) [ %s ] %s <- %s + 1 (%d)", index+1, displayLabel(), variableName, variableName, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }
}

class Decrease extends BaseCommand {
    String variableName;
    Decrease(String variableName, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        cycles = 1;
    }

    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        if (v.getValue() > 0) {
            v.setValue(v.getValue() - 1);
        }
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        return String.format("#%d (B) [ %s ] %s <- %s - 1 (%d)", index+1, displayLabel(), variableName, variableName, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }
}

class JumpNotZero extends BaseCommand {
    String variableName;
    String targetLabel;
    JumpNotZero(String variableName, String targetLabel, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        this.targetLabel = targetLabel;
        cycles = 2;
    }
    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        if (v.getValue() > 0) {
            if (targetLabel.equals(EXIT_LABEL)){
                programState.done = true;
                return;
            }
            programState.currentCommandIndex = programState.labelToIndex.get(targetLabel);
        }
        else{
            programState.currentCommandIndex++;
        }
    }

    @Override
    public String toString() {
        return String.format("#%d (B) [ %s ] IF %s != 0 GOTO %s (%d)", index+1, displayLabel(), variableName, targetLabel, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }
}

class Neutral extends BaseCommand {
    String variableName;
    Neutral(String variableName, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        cycles = 0;
    }
    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        return String.format("#%d (B) [ %s ] %s <- %s (%d)", index+1, displayLabel(), variableName, variableName, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }
}

class ZeroVariable extends BaseCommand {
    String variableName;
    ZeroVariable(String variableName, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        cycles = 1;
    }
    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        v.setValue(0);
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] %s <- 0 (%d)", index+1, displayLabel(), variableName, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }
}

class GotoLabel extends BaseCommand {
    String targetLabel;
    GotoLabel(String targetLabel, String label, int index) {
        super(label, index);
        this.targetLabel = targetLabel;
        cycles = 1;
    }
    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        if (targetLabel.equals(EXIT_LABEL)){
            programState.done = true;
            return;
        }
        programState.currentCommandIndex = programState.labelToIndex.get(targetLabel);
    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] GOTO %s (%d)", index+1, displayLabel(), targetLabel, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        return new HashSet<>();
    }
}

class Assignment extends BaseCommand {
    String variableName;
    String otherVariableName;
    Assignment(String variableName, String otherVariableName, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        this.otherVariableName = otherVariableName;
        cycles = 4;
    }

    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v1 = programState.variables.get(variableName);
        Variable v2 = programState.variables.get(otherVariableName);
        v1.setValue(v2.getValue());
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] %s <- %s (%d)", index+1, displayLabel(), variableName, otherVariableName, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        variables.add(otherVariableName);
        return variables;
    }

}

class ConstantAssignment extends BaseCommand {
    String variableName;
    int value;
    ConstantAssignment(String variableName, int value, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        this.value = value;
        cycles = 2;
    }

    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        v.setValue(value);
        programState.currentCommandIndex++;
    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] %s <- %d (%d)", index+1, displayLabel(), variableName, value, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }
}

class JumpZero extends BaseCommand {
    String variableName;
    String targetLabel;
    JumpZero(String variableName, String targetLabel, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        this.targetLabel = targetLabel;
        cycles = 2;
    }

    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        if (v.getValue() == 0) {
            if (targetLabel.equals(EXIT_LABEL)){
                programState.done = true;
                return;
            }
            programState.currentCommandIndex = programState.labelToIndex.get(targetLabel);
        }
        else{
            programState.currentCommandIndex++;
        }

    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] IF %s = 0 GOTO %s (%d)", index+1, displayLabel(), variableName, targetLabel, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }
}

class JumpEqualConstant extends BaseCommand {
    String variableName;
    int value;
    String targetLabel;

    JumpEqualConstant(String variableName, int value, String targetLabel, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        this.value = value;
        this.targetLabel = targetLabel;
        cycles = 2;
    }

    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += cycles;
        Variable v = programState.variables.get(variableName);
        if (v.getValue() == value) {
            if (targetLabel.equals(EXIT_LABEL)){
                programState.done = true;
                return;
            }
            programState.currentCommandIndex = programState.labelToIndex.get(targetLabel);
        }
        else{
            programState.currentCommandIndex++;
        }
    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] IF %s = %d GOTO %s (%d)", index+1, displayLabel(), variableName, value, targetLabel, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

}

class JumpEqualVariable extends BaseCommand {
    String variableName;
    String otherVariableName;
    String targetLabel;
    JumpEqualVariable(String variableName, String otherVariableName, String targetLabel, String label, int index) {
        super(label, index);
        this.variableName = variableName;
        this.otherVariableName = otherVariableName;
        this.targetLabel = targetLabel;
        cycles = 2;
    }

    @Override
    void execute(ProgramState programState) {
        programState.cyclesCount += 2;
        Variable v1 = programState.variables.get(variableName);
        Variable v2 = programState.variables.get(otherVariableName);
        if (v1.getValue() == v2.getValue()) {
            if (targetLabel.equals(EXIT_LABEL)){
                programState.done = true;
                return;
            }
            programState.currentCommandIndex = programState.labelToIndex.get(targetLabel);
        }
        else{
            programState.currentCommandIndex++;
        }
    }

    @Override
    public String toString() {
        return String.format("#%d (S) [ %s ] IF %s = %s GOTO %s (%d)", index+1, displayLabel(), variableName, otherVariableName, targetLabel, cycles);
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        variables.add(otherVariableName);
        return variables;
    }
}