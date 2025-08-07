package program;

abstract class BaseCommand {
    abstract void execute(ProgramState programState);
    @Override
    abstract public String toString();
    //abstract List<BaseCommands> extend();
    final String NO_LABEL = "-1";
    final String EXIT_LABEL = "Exit";
    String label;
    int index;
    int cycles;
    protected BaseCommand(String label, int index) {
        setLabel(label);
        this.index = index;
    }
    void setLabel(String label) {
        if(label.equals(NO_LABEL)) {
            label = "   ";
        }
        else if(label.length() == 2){
            label += " ";
        }
        this.label = label;
    }
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

        return String.format("#%d (B) [ %s ] %s <- %s + 1 (%d)", index+1, label, variableName, variableName, cycles);
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
        return String.format("#%d (B) [ %s ] %s <- %s - 1 (%d)", index+1, label, variableName, variableName, cycles);
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
        return String.format("#%d (B) [ %s ] IF %s != 0 GOTO %s (%d)", index+1, label, variableName, targetLabel, cycles);
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
        return String.format("#%d (B) [ %s ] %s <- %s (%d)", index+1, label, variableName, variableName, cycles);
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
        return String.format("#%d (S) [ %s ] %s <- 0 (%d)", index+1, label, variableName, cycles);
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
        return String.format("#%d (S) [ %s ] GOTO %s (%d)", index+1, label, targetLabel, cycles);
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
        return String.format("#%d (S) [ %s ] %s <- %s (%d)", index+1, label, variableName, otherVariableName, cycles);
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
        return String.format("#%d (S) [ %s ] %s <- %d (%d)", index+1, label, variableName, value, cycles);
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
        return String.format("#%d (S) [ %s ] IF %s = 0 GOTO %s (%d)", index+1, label, variableName, targetLabel, cycles);
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
        return String.format("#%d (S) [ %s ] IF %s = %d GOTO %s (%d)", index+1, label, variableName, value, targetLabel, cycles);
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
        return String.format("#%d (S) [ %s ] IF %s = %s GOTO %s (%d)", index+1, label, variableName, otherVariableName, targetLabel, cycles);
    }
}