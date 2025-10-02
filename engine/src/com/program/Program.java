package com.program;

import com.XMLHandlerV2.SInstruction;
import com.api.ProgramResult;
import com.api.ProgramSummary;
import com.commands.BaseCommand;
import com.commands.CommandFactory;
import com.commands.Variable;
import com.dto.CommandTreeNodeDto;
import com.dto.ProgramTreeDto;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Program implements Serializable {
    String name;
    List<com.commands.BaseCommand> commands;
    HashSet<String> presentVariables;
    HashMap<String, Integer> labelToIndex;
    List<String> inputVariables;
    List<String> labels;
    boolean isMidDebug = false;
    ProgramState debugState = null;
    ProgramSummary summary = null;
    // Tracks in-place expansions for mixed tree view: each span replaces a single parent
    private final List<ExpansionSpan> expansionSpans = new ArrayList<>();

    public Program(String name, List<com.commands.BaseCommand> commands){
        this.name = name;
        this.commands = commands;
        unpackCommands();
        createSummary(commands);
    }

    public Program(Program other){
        this.name = other.name;
        commands = new ArrayList<>();
        for(BaseCommand command : other.getCommands()){
            commands.add(command.copy(command.getPresentVariables(), command.getConstantsForCopy(), command.getLabelsForCopy(), command.getIndex(), command.getCreator()));
        }
        unpackCommands();
        createSummary(commands);
    }

    private void createSummary(List<com.commands.BaseCommand> commands){
        int baseCommands = Math.toIntExact(commands.stream().filter(BaseCommand::isBaseCommand).count());
        summary = new ProgramSummary(baseCommands, commands.size() - baseCommands);
    }

    public ProgramSummary getSummary(){
        return summary;
    }

    public void addCommand(com.commands.BaseCommand command){
        commands.add(command);
        // Keep derived structures and summary up to date after mutations
        unpackCommands();
        createSummary(commands);
    }

    /*
        The method receives input for the program, runs it, and return the programState.
        Result at programState.variables.get("y")
        Cycles count at programState.cyclesCount
     */
    public String getName() {return name;}
    public ProgramResult execute(List<Integer> input){
        ProgramState programState = new ProgramState(input, presentVariables, commands, labelToIndex);
        while (!programState.done && programState.currentCommandIndex < commands.size()){
            com.commands.BaseCommand command = commands.get(programState.currentCommandIndex);
            command.execute(programState);
        }
        return new ProgramResult(programState.cyclesCount, variableToValue(programState), programState.currentCommandIndex, false);
    }

    public ProgramResult startDebug(List<Integer> input, List<Integer> breakpoints){
        ProgramState programState = new ProgramState(input, presentVariables, commands, labelToIndex);
        programState.initialBreakpoints(breakpoints);
        return runToBreakpoint(programState);
    }

    public ProgramResult stepOver(){
        ProgramState programState = debugState;
        com.commands.BaseCommand command = commands.get(programState.currentCommandIndex);
        command.execute(programState);
        boolean isDebug;
        isDebug = !programState.done && programState.currentCommandIndex < commands.size();
        debugState = programState;
        return new ProgramResult(programState.cyclesCount, variableToValue(programState), programState.currentCommandIndex, isDebug);
    }

    public ProgramResult continueDebug(){
        //Always perform at least 1 step, and then continue debugging
        ProgramState programState = debugState;
        com.commands.BaseCommand command = commands.get(programState.currentCommandIndex);
        command.execute(programState);
        if(programState.done || programState.currentCommandIndex >= commands.size()){
            isMidDebug = false;
            debugState = null;
            return new ProgramResult(programState.cyclesCount, variableToValue(programState), programState.currentCommandIndex, false);
        }
        return runToBreakpoint(programState);
    }

    private ProgramResult runToBreakpoint(ProgramState programState) {
        while(!programState.done && programState.currentCommandIndex < commands.size()){
            if(programState.breakpoints[programState.currentCommandIndex]){
                isMidDebug = true;
                debugState = programState;
                return new ProgramResult(programState.cyclesCount, variableToValue(programState), programState.currentCommandIndex, true);
            }
            BaseCommand command = commands.get(programState.currentCommandIndex);
            command.execute(programState);
        }
        isMidDebug = false;
        debugState = null;
        return new ProgramResult(programState.cyclesCount, variableToValue(programState), programState.currentCommandIndex, false);
    }

    public void stopDebug(){
        isMidDebug = false;
        debugState = null;
    }

    // Package current debug state into a finished ProgramResult for statistics
    public ProgramResult snapshotDebugAsFinished(){
        if (debugState == null){
            throw new IllegalStateException("No debug state to snapshot");
        }
        return new ProgramResult(debugState.cyclesCount, variableToValue(debugState), debugState.currentCommandIndex, false);
    }

    public ProgramResult stepBack(){
        ProgramState programState = debugState;
        if (programState.singleStepChanges.isEmpty()){
            throw new IllegalStateException("No steps to undo");
        }
        SingleStepChanges singleStepChanges = programState.singleStepChanges.pop();
        programState.cyclesCount = singleStepChanges.getCyclesChange().oldValue();
        programState.currentCommandIndex = singleStepChanges.getIndexChange().oldValue();
        programState.variables.get(singleStepChanges.getVariableChanges().variable()).setValue(singleStepChanges.getVariableChanges().oldValue());
        return new ProgramResult(programState.cyclesCount, variableToValue(programState), programState.currentCommandIndex, true);
    }

    private int getMaxWorkVariable(){
        int max = 0;
        for (String variables : presentVariables) {
            if (variables.charAt(0) == 'z'){
                max = Math.max(max, Integer.parseInt(variables.substring(1)));
            }
        }

        return max;
    }
    private int getMaxLabel(){
        int max = 0;
        for (String label : labels) {
            if (label.charAt(0) == 'L'){
                max = Math.max(max, Integer.parseInt(label.substring(1)));
            }
        }
        return max;
    }

    void unpackCommands(){
        inputVariables = new ArrayList<>();
        presentVariables = new HashSet<>();
        labelToIndex = new HashMap<>();
        labels = new ArrayList<>();
        for(int i = 0; i < commands.size(); i++){
            com.commands.BaseCommand command = commands.get(i);
            if(!command.getLabel().equals(com.commands.BaseCommand.NO_LABEL)){
                //The only label that matters is the first of its kind in the program.
                if(!labelToIndex.containsKey(command.getLabel())){
                    labelToIndex.put(command.getLabel(), i);
                }
                labels.add(command.getLabel());
            }
            List<String> commandVariables = command.getPresentVariables();
            presentVariables.addAll(commandVariables);
            for(String variable : commandVariables){
                if(variable.startsWith("x") && !inputVariables.contains(variable)){
                    inputVariables.add(variable);
                }
            }
        }
    }

    public Program expand(int level){
        List<BaseCommand> currentCommands = commands;
        AtomicInteger nextAvailableLabel = new AtomicInteger(getMaxLabel()+1);
        AtomicInteger nextAvailableVariable = new AtomicInteger(getMaxWorkVariable()+1);
        AtomicInteger realIndex = new AtomicInteger(0);
        for(int i = 0; i < level; i++){
            List<BaseCommand> newCommands = new ArrayList<>();
            realIndex.set(0);
            for(BaseCommand command : currentCommands){
                newCommands.addAll(command.expand(nextAvailableVariable, nextAvailableLabel, realIndex));
            }
            currentCommands = newCommands;
        }

        return new Program(name, currentCommands);
    }

    public int getMaxExpansionLevel(){
        return commands.stream()
                .mapToInt(com.commands.BaseCommand::getExpansionLevel)
                .max()
                .orElse(0);
    }

    public void verifyLegal(){
        for(com.commands.BaseCommand command : commands){
            String targetLabel = command.getTargetLabel();
            if(!targetLabel.equals(com.commands.BaseCommand.NO_LABEL) && !targetLabel.equals(com.commands.BaseCommand.EXIT_LABEL) && !labelToIndex.containsKey(targetLabel)){
                throw new IllegalArgumentException("Target Label is not in list");
            }
        }
    }

    private HashMap<String,Integer> variableToValue(ProgramState state){
        return (HashMap<String, Integer>) state.variables.values().stream()
                .collect(Collectors.toMap(Variable::getName, Variable::getValue));
    }

    public List<String> getInputVariables() {
        return Collections.unmodifiableList(inputVariables);
    }


    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("Input variables used in the program (in order): ");
        for (String inputVariable : inputVariables){
            sb.append(inputVariable).append(", ");
        }

        //Remove the last ", "
        if(!inputVariables.isEmpty()){
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("\n");
        sb.append("The labels used in the program (in order): ");
        for(String label : labels){
            sb.append(label).append(", ");
        }

        //Remove the last ", "
        if(!labels.isEmpty()){
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("\n");
        sb.append("The program: \n");
        for(com.commands.BaseCommand command : commands){
            sb.append(command.toString()).append("\n");
        }

        return sb.toString();
    }

    public HashSet<String> getPresentVariables() {
        return new HashSet<>(presentVariables);
    }

    public List<String> getLabels() {
        return new ArrayList<>(labelToIndex.keySet());
    }

    public List<BaseCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public static Program createProgram(String name, List<SInstruction> instructions) {
        List<BaseCommand> commands = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            SInstruction ins = instructions.get(i);

            commands.add(
                    CommandFactory.createCommand(
                            ins.getName(),
                            ins.getSVariable(),
                            ins.getSLabel(),
                            ins.getSInstructionArguments() == null
                                    ? null
                                    : ins.getSInstructionArguments().getSInstructionArgument(),
                            i
                    )
            );
        }

        Program program = new Program(name, commands);
        program.verifyLegal();
        return program;
    }


    public void setBreakpoint(int index) {
        if(debugState != null){
            debugState.setBreakPoint(index);
        }
    }

    public void removeBreakpoint(int index) {
        if(debugState != null){
            debugState.removeBreakPoint(index);
        }
    }

    // ===== Mixed expansion support (visual-only) =====
    private static final class ExpansionSpan {
        final BaseCommand parent;
        int startInclusive;
        int endExclusive;

        ExpansionSpan(BaseCommand parent, int startInclusive, int endExclusive){
            this.parent = parent;
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
        }

        int length(){
            return endExclusive - startInclusive;
        }
    }

    public void expandAt(int index){
        if (index < 0 || index >= commands.size()){
            throw new IndexOutOfBoundsException("expandAt: index out of bounds: " + index);
        }

        BaseCommand parent = commands.get(index);

        // Counters start from current maxima for correctness
        AtomicInteger nextAvailableLabel = new AtomicInteger(getMaxLabel() + 1);
        AtomicInteger nextAvailableVariable = new AtomicInteger(getMaxWorkVariable() + 1);
        AtomicInteger realIndex = new AtomicInteger(index);

        List<BaseCommand> children = parent.expand(nextAvailableVariable, nextAvailableLabel, realIndex);
        if (children == null || children.isEmpty()){
            return; // nothing to expand
        }

        // Replace the single parent with its expanded children
        commands.remove(index);
        commands.addAll(index, children);

        // Register the new span and adjust existing spans after insertion point
        int inserted = children.size();
        registerExpansionSpan(parent, index, index + inserted);
        shiftSpansAfterInsert(index, inserted - 1);

        // Refresh derived structures
        reindexCommands();
        unpackCommands();
        createSummary(commands);
    }

    public void collapseAt(int index){
        if (index < 0 || index >= commands.size()){
            throw new IndexOutOfBoundsException("collapseAt: index out of bounds: " + index);
        }
        if (expansionSpans.isEmpty()){
            return; // nothing to collapse
        }

        ExpansionSpan target = findInnermostSpanContaining(index);
        if (target == null){
            return; // index not inside any expanded span
        }

        int start = target.startInclusive;
        int end = target.endExclusive;
        int spanLen = end - start;

        // Remove nested spans within [start, end)
        removeNestedSpans(start, end);

        // Replace the expanded region with the original parent command
        // First, remove the region
        for (int i = 0; i < spanLen; i++){
            commands.remove(start);
        }
        // Then, insert the parent back
        commands.add(start, target.parent);

        // Remove the target span entry
        expansionSpans.remove(target);

        // Shift spans after the collapsed region by delta = 1 - spanLen
        int delta = 1 - spanLen;
        shiftSpansAfterCollapse(end, delta);

        // Refresh derived structures
        reindexCommands();
        unpackCommands();
        createSummary(commands);
    }

    private void registerExpansionSpan(BaseCommand parent, int startInclusive, int endExclusive){
        expansionSpans.add(new ExpansionSpan(parent, startInclusive, endExclusive));
    }

    private void shiftSpansAfterInsert(int insertionIndex, int delta){
        if (delta == 0) return;
        for (ExpansionSpan span : expansionSpans){
            // Skip the span that starts exactly at insertionIndex (the just-added one),
            // we want to shift spans that are strictly after the replaced parent position
            if (span.startInclusive > insertionIndex){
                span.startInclusive += delta;
                span.endExclusive += delta;
            }
        }
    }

    private void shiftSpansAfterCollapse(int collapsedEndExclusive, int delta){
        if (delta == 0) return;
        for (ExpansionSpan span : expansionSpans){
            if (span.startInclusive >= collapsedEndExclusive){
                span.startInclusive += delta;
                span.endExclusive += delta;
            }
        }
    }

    private void removeNestedSpans(int startInclusive, int endExclusive){
        // Remove all spans fully contained within [start, end)
        expansionSpans.removeIf(s -> s.startInclusive >= startInclusive && s.endExclusive <= endExclusive);
    }

    private ExpansionSpan findInnermostSpanContaining(int index){
        ExpansionSpan best = null;
        for (ExpansionSpan span : expansionSpans){
            if (span.startInclusive <= index && index < span.endExclusive){
                if (best == null || span.length() < best.length()){
                    best = span;
                }
            }
        }
        return best;
    }

    private void reindexCommands(){
        for (int i = 0; i < commands.size(); i++){
            commands.get(i).setIndex(i);
        }
    }

    private boolean belongsToAncestor(BaseCommand node, BaseCommand ancestor){
        BaseCommand cur = node;
        while (cur != null){
            if (cur == ancestor) return true;
            cur = cur.getCreator();
        }
        return false;
    }

    // ===== Tree DTO builder =====
    public ProgramTreeDto buildTreeDto(){
        List<CommandTreeNodeDto> roots = new ArrayList<>();
        long[] idGen = new long[]{1L};

        // Build one parent node per command; if expanded span exists, attach children
        for (int i = 0; i < commands.size(); ){
            BaseCommand cmd = commands.get(i);
            ExpansionSpan span = findSpanStartingAt(i);
            List<CommandTreeNodeDto> children = new ArrayList<>();
            boolean isExpanded = false;
            int advance = 1;
            if (span != null){
                isExpanded = true;
                advance = span.endExclusive - span.startInclusive;
                for (int j = span.startInclusive; j < span.endExclusive; ){
                    ExpansionSpan inner = findSpanStartingAt(j);
                    if (inner != null){
                        // child itself is expanded; attach its grandchildren
                        BaseCommand childCmd = commands.get(j);
                        List<CommandTreeNodeDto> grand = new ArrayList<>();
                        for (int k = inner.startInclusive; k < inner.endExclusive; k++){
                            BaseCommand grandCmd = commands.get(k);
                            grand.add(new CommandTreeNodeDto(
                                    idGen[0]++,
                                    List.of(i, j - span.startInclusive, k - inner.startInclusive),
                                    grandCmd.toDisplayString(),
                                    grandCmd.getLabel(),
                                    grandCmd.isBaseCommand(),
                                    false,
                                    List.of()
                            ));
                        }
                        children.add(new CommandTreeNodeDto(
                                idGen[0]++,
                                List.of(i, j - span.startInclusive),
                                childCmd.toDisplayString(),
                                childCmd.getLabel(),
                                childCmd.isBaseCommand(),
                                true,
                                grand
                        ));
                        j = inner.endExclusive;
                    } else {
                        BaseCommand child = commands.get(j);
                        children.add(new CommandTreeNodeDto(
                                idGen[0]++,
                                List.of(i, j - span.startInclusive),
                                child.toDisplayString(),
                                child.getLabel(),
                                child.isBaseCommand(),
                                false,
                                List.of()
                        ));
                        j++;
                    }
                }
            }

            // Parent node represents the command header and is always synthetic-expandable in the tree
            CommandTreeNodeDto parentNode = new CommandTreeNodeDto(
                    idGen[0]++,
                    List.of(i),
                    cmd.toDisplayString(),
                    cmd.getLabel(),
                    cmd.isBaseCommand(),
                    isExpanded,
                    children
            );
            roots.add(parentNode);
            i += advance;
        }

        return new ProgramTreeDto(name, roots);
    }

    private ExpansionSpan findSpanStartingAt(int startIndex){
        for (ExpansionSpan span : expansionSpans){
            if (span.startInclusive == startIndex){
                return span;
            }
        }
        return null;
    }

    // Resolve a path from the tree DTO to the current flat commands index
    public int resolveIndexFromPath(List<Integer> path){
        if (path == null || path.isEmpty()){
            throw new IllegalArgumentException("Path must not be empty");
        }
        int rootIndex = path.get(0);
        if (path.size() == 1){
            return rootIndex;
        }
        ExpansionSpan span = findSpanStartingAt(rootIndex);
        if (span == null){
            return rootIndex;
        }
        int childOffset = path.get(1);
        int idx = span.startInclusive + childOffset;
        if (idx < span.startInclusive || idx >= span.endExclusive){
            throw new IndexOutOfBoundsException("Resolved child index out of span bounds");
        }
        return idx;
    }
}
