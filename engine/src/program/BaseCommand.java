package program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

abstract class BaseCommand {
    abstract void execute(ProgramState programState);
    @Override
    abstract public String toString();
    //abstract List<BaseCommands> extend();
    static final String NO_LABEL = "-1";
    static final String EXIT_LABEL = "EXIT";
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

    protected void verifyLabel(String label) {
        if (!label.equals(EXIT_LABEL) && label.matches("L\\d\\d")) {
            throw new IllegalArgumentException("Invalid label format");
        }
    }

    protected void appendCreators(StringBuilder sb){
        BaseCommand curCreator = creator;
        while (curCreator != null){
            sb.append(" <<< ").append(curCreator.toString());
            curCreator = curCreator.creator;
        }
    }


    abstract HashSet<String>  getPresentVariables();
    abstract List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex);
    abstract int getExpansionLevel();
}

class Increase extends BaseCommand {
    String variableName;
    Increase(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (B) [ %s ] %s <- %s + 1 (%d)", index+1, displayLabel(), variableName, variableName, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        return List.of(new Increase(variableName, label, realIndex.getAndIncrement(), creator));
    }

    @Override
    int getExpansionLevel() {
        return 0;
    }
}

class Decrease extends BaseCommand {
    String variableName;
    Decrease(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (B) [ %s ] %s <- %s - 1 (%d)", index+1, displayLabel(), variableName, variableName, cycles));
        BaseCommand curCreator = creator;
        while (curCreator != null){
            sb.append(" <<< ").append(curCreator.toString());
            curCreator = curCreator.creator;
        }

        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        return List.of(new Decrease(variableName, label, realIndex.getAndIncrement(), creator));
    }

    @Override
    int getExpansionLevel() {
        return 0;
    }
}

class JumpNotZero extends BaseCommand {
    String variableName;
    String targetLabel;
    JumpNotZero(String variableName, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        this.variableName = variableName;
        this.targetLabel = targetLabel;
        verifyLabel(targetLabel);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (B) [ %s ] IF %s != 0 GOTO %s (%d)", index+1, displayLabel(), variableName, targetLabel, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        return List.of(new JumpNotZero(variableName, targetLabel, label, realIndex.getAndIncrement(), creator));
    }

    @Override
    int getExpansionLevel() {
        return 0;
    }
}

class Neutral extends BaseCommand {
    String variableName;
    Neutral(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (B) [ %s ] %s <- %s (%d)", index+1, displayLabel(), variableName, variableName, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        return List.of(new Neutral(variableName, label, realIndex.getAndIncrement(), creator));
    }

    @Override
    int getExpansionLevel() {
        return 0;
    }
}

class ZeroVariable extends BaseCommand {
    String variableName;
    ZeroVariable(String variableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] %s <- 0 (%d)", index+1, displayLabel(), variableName, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if(level == 0){
            return List.of(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L" + nextAvailableLabel.getAndIncrement();
        commands.add(new Neutral(variableName, label, realIndex.getAndIncrement(),this));
        commands.add(new Decrease(variableName, L1, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(variableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    int getExpansionLevel() {
        return 1;
    }
}

class GotoLabel extends BaseCommand {
    String targetLabel;
    GotoLabel(String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] GOTO %s (%d)", index+1, displayLabel(), targetLabel, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        return new HashSet<>();
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if(level == 0){
            return List.of(new GotoLabel(targetLabel, label, realIndex.getAndIncrement(), creator));
        }
        String nextVar = "z" + nextAvailableLabel.getAndIncrement();
        List<BaseCommand> commands = new ArrayList<>();
        commands.add(new Increase(nextVar, label, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(nextVar, targetLabel, NO_LABEL, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    int getExpansionLevel() {
        return 1;
    }
}

class Assignment extends BaseCommand {
    String variableName;
    String otherVariableName;
    Assignment(String variableName, String otherVariableName, String label, int index, BaseCommand creator) {
        super(label, index, creator);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] %s <- %s (%d)", index+1, displayLabel(), variableName, otherVariableName, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        variables.add(otherVariableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if(level == 0){
            return List.of(new Assignment(variableName, otherVariableName, label, realIndex.getAndIncrement(), creator));
        }
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        String L2 = "L"+ nextAvailableLabel.getAndIncrement();
        String L3 = "L"+ nextAvailableLabel.getAndIncrement();
        String z1 = "z"+ nextAvailableLabel.getAndIncrement();
        List<BaseCommand> commands = new ArrayList<>();
        commands.addAll(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new JumpNotZero(otherVariableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.addAll(new GotoLabel(L3, NO_LABEL, realIndex.getAndIncrement(), this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new Decrease(otherVariableName, L1, realIndex.getAndIncrement(), this));
        commands.add(new Increase(z1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(otherVariableName, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Decrease(z1, L2, realIndex.getAndIncrement(), this));
        commands.add(new Increase(variableName, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Increase(otherVariableName, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new JumpNotZero(z1, L2, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.add(new Neutral(variableName, L3, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    int getExpansionLevel() {
        return 2;
    }

}

class ConstantAssignment extends BaseCommand {
    String variableName;
    int value;
    ConstantAssignment(String variableName, int value, String label, int index, BaseCommand creator) {
        super(label, index, creator);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] %s <- %d (%d)", index+1, displayLabel(), variableName, value, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if(level == 0){
            return List.of(new ConstantAssignment(variableName, value, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        commands.addAll(new ZeroVariable(variableName, label, realIndex.getAndIncrement(), this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        for(int i = 0; i < value; i++){
            commands.add(new Increase(variableName, NO_LABEL, realIndex.getAndIncrement(), this));
        }
        return commands;
    }

    @Override
    int getExpansionLevel() {
        return 2;
    }
}

class JumpZero extends BaseCommand {
    String variableName;
    String targetLabel;
    JumpZero(String variableName, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] IF %s = 0 GOTO %s (%d)", index+1, displayLabel(), variableName, targetLabel, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if(level == 0){
            return List.of(new JumpZero(variableName, targetLabel, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        commands.add(new JumpNotZero(variableName, L1, label, realIndex.getAndIncrement(), this));
        commands.addAll(new GotoLabel(targetLabel, NO_LABEL, realIndex.getAndIncrement(), this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new Neutral(variableName, L1, realIndex.getAndIncrement(), this));
        return commands;
    }

    @Override
    int getExpansionLevel() {
        return 2;
    }
}

class JumpEqualConstant extends BaseCommand {
    String variableName;
    int value;
    String targetLabel;

    JumpEqualConstant(String variableName, int value, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] IF %s = %d GOTO %s (%d)", index+1, displayLabel(), variableName, value, targetLabel, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if(level == 0){
            return List.of(new JumpEqualConstant(variableName, value, targetLabel, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        String z1 = "Z"+ nextAvailableLabel.getAndIncrement();
        commands.addAll(new Assignment(z1, variableName, label, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        for(int i = 0; i < value; i++){
            commands.addAll(new JumpZero(z1, L1, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
            commands.add(new Decrease(z1,NO_LABEL, realIndex.getAndIncrement(), this));
        }
        commands.add(new JumpNotZero(z1, L1, NO_LABEL, realIndex.getAndIncrement(), this));
        commands.addAll(new GotoLabel(targetLabel, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new Neutral(variableName, L1, realIndex.getAndIncrement(),this));
        return commands;
    }

    @Override
    int getExpansionLevel() {
        return 3;
    }

}

class JumpEqualVariable extends BaseCommand {
    String variableName;
    String otherVariableName;
    String targetLabel;
    JumpEqualVariable(String variableName, String otherVariableName, String targetLabel, String label, int index, BaseCommand creator) {
        super(label, index, creator);
        verifyLabel(targetLabel);
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#%d (S) [ %s ] IF %s = %s GOTO %s (%d)", index+1, displayLabel(), variableName, otherVariableName, targetLabel, cycles));
        appendCreators(sb);
        return sb.toString();
    }

    @Override
    HashSet<String> getPresentVariables() {
        HashSet<String> variables = new HashSet<>();
        variables.add(variableName);
        variables.add(otherVariableName);
        return variables;
    }

    @Override
    List<BaseCommand> expand(int level, AtomicInteger nextAvailableVariable, AtomicInteger nextAvailableLabel, AtomicInteger realIndex) {
        if (level == 0){
            return List.of(new JumpEqualVariable(variableName, otherVariableName, targetLabel, label, realIndex.getAndIncrement(), creator));
        }
        List<BaseCommand> commands = new ArrayList<>();
        String L1 = "L"+ nextAvailableLabel.getAndIncrement();
        String L2 = "L"+ nextAvailableLabel.getAndIncrement();
        String L3 = "L"+ nextAvailableLabel.getAndIncrement();
        String z1 = "Z"+ nextAvailableVariable.getAndIncrement();
        String z2 = "Z"+ nextAvailableVariable.getAndIncrement();

        commands.addAll(new Assignment(z1, variableName, label, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new Assignment(z2, otherVariableName, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new JumpZero(z1, L3, L2, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new JumpZero(z2, L1, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new Decrease(z1, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.add(new Decrease(z2, NO_LABEL, realIndex.getAndIncrement(),this));
        commands.addAll(new GotoLabel(L2, NO_LABEL, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.addAll(new JumpZero(z2, targetLabel, L3, realIndex.getAndIncrement(),this).expand(level-1, nextAvailableVariable, nextAvailableLabel, realIndex));
        commands.add(new Neutral(variableName, L1, realIndex.getAndIncrement(),this));
        return commands;
    }

    @Override
    int getExpansionLevel() {
        return 3;
    }
}