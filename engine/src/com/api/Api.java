package com.api;
import com.XMLHandlerV2.SProgram;
import com.XMLHandlerV2.SInstruction;
import com.XMLHandlerV2.SInstructionArgument;
import com.XMLHandlerV2.SInstructionArguments;
import com.commands.BaseCommand;
import com.commands.CommandFactory;
import com.commands.CommandMetadata;
import com.commands.ReverseFactory;
import com.commands.FnArgs;
import com.dto.CommandSchemaDto;
import com.dto.ProgramTreeDto;
import com.program.MixedExpansionSession;
import com.program.Program;
import com.program.ProgramState;
import com.program.Architecture;
import com.commands.FunctionRegistry;
import com.dto.api.ProgramResult;
import com.dto.api.ProgramSummary;
import com.dto.api.Statistic;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Api {
    private Program curProgram;
    private Program debugProgram;
    private List<Integer> debugInput;
    private int debugExpansionLevel;
    // Mixed tree view state (visual-only)
    private MixedExpansionSession mixedSession;
    private String userId;
    private int credits;
    private int usedCredits;
    private int chargedDebugCycles;
    private int currentRunOverhead;
    private String currentRunArchitecture;
    private int programsRanCount;
    
    public Api(String userId){
        this.userId = userId;
        this.usedCredits = 0;
        this.programsRanCount = 0;
    }

    public String getCurProgramName() {
        return curProgram.getName();
    }

    public int getCredits(){
        return credits;
    }

    public int getUsedCredits(){
        return usedCredits;
    }

    public int getProgramsRanCount(){
        return programsRanCount;
    }

    public void addCredits(int amount){
        credits += amount;
    }

    public void loadSProgram(InputStream xmlStream) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(SProgram.class);
        Unmarshaller um = ctx.createUnmarshaller();
        SProgram sp = ((SProgram) um.unmarshal(xmlStream));
        // Pre-validate program name uniqueness prior to any registration
        FnArgs.assertProgramNameAvailable(this.userId, sp.getName());
        // Phase 1: register function names/arity atomically; will throw without partial writes
        CommandFactory.registerFunctions(this.userId, sp.getSFunctions(), sp.getName());
        // Build and register program only after successful validation/registration
        Program p = Program.createProgram(sp.getName(), sp.getSInstructions().getSInstruction());
        FnArgs.registerProgram(this.userId, p);
    }
    public void createEmptyProgram(String name){
        curProgram = Program.createProgram(name, Collections.emptyList());
        FnArgs.registerProgram(this.userId, curProgram);
    }


    public ProgramResult executeProgram(List<Integer> input, int expansionLevel, String architecture){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        // Architecture validation
        ensureArchitectureAllowed(p, architecture);
        int overhead = getArchitectureOverhead(architecture);
        // Early credit gates
        if (credits < overhead){
            throw new IllegalStateException("Insufficient credits for architecture overhead");
        }
        double avg = FunctionRegistry.getAverageCost(p.getName()).orElse(0.0);
        if (avg + overhead > credits){
            throw new IllegalStateException("Insufficient credits: average run cost plus overhead exceeds available credits");
        }
        // Charge overhead and execute with remaining credits as budget
        credits -= overhead;
        usedCredits += overhead;
        ProgramResult res = p.executeWithBudget(input, credits);
        // Charge cycles consumed
        credits -= res.getCycles();
        usedCredits += res.getCycles();
        // Save statistics and global averages (overhead + cycles)
        Statistic.saveRunDetails(userId, expansionLevel, architecture, input, res.getResult(), res.getCycles(), res.getVariableToValue());
        FunctionRegistry.recordRunCost(p.getName(), overhead + res.getCycles());
        programsRanCount += 1;
        return res;
    }

    public String getProgram(int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        return p.toString();
    }

    public boolean isLoaded(){
        return curProgram != null;
    }

    public int getMaxLevel(){
        return curProgram.getMaxExpansionLevel();
    }

    void setCurProgram(Program curProgram){
        this.curProgram = curProgram;
    }

    public List<String> getInputVariableNames(){
        return curProgram.getInputVariables();
    }

    public List<String> getProgramCommands(int expansionLevel){
        return curProgram.expand(expansionLevel).getCommands().stream().map(BaseCommand::toString).toList();
    }

    public static List<String> getAvailableFunctions(){
        return FnArgs.getFunctionNames();
    }

    // ===== Mixed Tree View API (visual-only) =====
    public void startMixedTreeView(){
        if (curProgram == null){
            throw new IllegalStateException("No program loaded");
        }
        mixedSession = MixedExpansionSession.buildFromProgram(new Program(curProgram));
    }

    public ProgramTreeDto getMixedTree(){
        if (mixedSession == null){
            throw new IllegalStateException("Mixed tree view not initialized");
        }
        return mixedSession.toDto();
    }

    public ProgramTreeDto expandMixedAt(List<Integer> path){
        if (mixedSession == null){
            throw new IllegalStateException("Mixed tree view not initialized");
        }
        mixedSession.expandByPath(path);
        return mixedSession.toDto();
    }

    public ProgramTreeDto collapseMixedAt(List<Integer> path){
        if (mixedSession == null){
            throw new IllegalStateException("Mixed tree view not initialized");
        }
        mixedSession.collapseByPath(path);
        return mixedSession.toDto();
    }

    // Program copy uses Program's copy constructor; no binary deep copy needed

    // ===== Schema exposure for UI =====
    public static List<String> listCommandNames() {
        return new ArrayList<>(CommandMetadata.getSupportedCommandNames());
    }

    public static CommandSchemaDto getCommandSchema(String commandName) {
        return CommandMetadata.getSchema(commandName).toDto();
    }

    // ===== Create and append command from UI inputs =====
    public void createAndAddCommand(
            String commandName,
            String variable,
            String label,
            Map<String, String> args
    ) {
        if (curProgram == null) {
            throw new IllegalStateException("No program loaded");
        }
        int index = curProgram.getCommands().size();

        // Build SInstruction compatible with XML handler and factory
        SInstruction ins = new SInstruction();
        ins.setName(commandName);
        // instruction type is not used by factory; schema enforces correctness when saving to XML
        ins.setSVariable(variable);
        if (label != null && !label.isBlank()) {
            ins.setSLabel(label);
        }
        if (args != null && !args.isEmpty()) {
            SInstructionArguments sArgs = new SInstructionArguments();
            for (Map.Entry<String, String> e : args.entrySet()) {
                SInstructionArgument a = new SInstructionArgument();
                a.setName(e.getKey());
                a.setValue(e.getValue());
                sArgs.getSInstructionArgument().add(a);
            }
            ins.setSInstructionArguments(sArgs);
        }

        BaseCommand cmd = CommandFactory.createCommand(
                ins.getName(),
                ins.getSVariable(),
                ins.getSLabel(),
                ins.getSInstructionArguments() == null ? null : ins.getSInstructionArguments().getSInstructionArgument(),
                index
        );
        curProgram.addCommand(cmd);
    }

    public static List<String> getFunctionCommands(String functionName, int expansionLevel){
        Program p = FnArgs.getProgramByName(functionName).expand(expansionLevel);
        return p.getCommands().stream().map(BaseCommand::toString).toList();
    }

    // Save current program to XML under the given folder path; returns absolute file path
    public String saveCurrentProgramAsXml(String folderPath){
        if(curProgram == null){
            throw new IllegalStateException("No program is loaded.");
        }
        Path folder = Paths.get(folderPath);
        Path file = ReverseFactory.saveProgramToXml(folder, curProgram);
        return file.toAbsolutePath().toString();
    }

    public void setCurProgram(String functionName){
        curProgram = FnArgs.getProgramByName(functionName);
    }

    public static List<String> getStateFileNames(String path){
        File folder = new File(path);
        if(!folder.exists() || !folder.isDirectory()){
            throw new IllegalArgumentException("The path provided is not a directory.\n" + path);
        }

        File[] listOfFiles = folder.listFiles();
        if(listOfFiles == null || listOfFiles.length == 0){
            throw new IllegalArgumentException("No state files in folder: " + path);
        }

        List<String> stateFileNames = new ArrayList<>();
        for(File file : listOfFiles){
            if(file.isFile() && file.getName().endsWith(".dat")){
                stateFileNames.add(file.getAbsolutePath());
            }
        }

        return stateFileNames;
    }

    // Full system save/load removed

    public static void loadState(String path){ throw new UnsupportedOperationException("Full system save/load removed"); }

    public ProgramResult startDebugging(List<Integer> input, int expansionLevel, List<Integer> breakpoints, String architecture){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        // Architecture validation and credit gates
        ensureArchitectureAllowed(p, architecture);
        int overhead = getArchitectureOverhead(architecture);
        if (credits < overhead){
            throw new IllegalStateException("Insufficient credits for architecture overhead");
        }
        double avg = FunctionRegistry.getAverageCost(p.getName()).orElse(0.0);
        if (avg + overhead > credits){
            throw new IllegalStateException("Insufficient credits: average run cost plus overhead exceeds available credits");
        }
        // Charge overhead and start debug with budget
        credits -= overhead;
        usedCredits += overhead;
        currentRunOverhead = overhead;
        ProgramResult res = p.startDebugWithBudget(input, breakpoints, credits);
        // Debit cycles executed up to initial breakpoint/end
        if (res.getCycles() > 0){
            credits -= res.getCycles();
            usedCredits += res.getCycles();
        }
        if(!res.isDebug()){
            // finished immediately
            Statistic.saveRunDetails(userId, expansionLevel, architecture, input, res.getResult(), res.getCycles(), res.getVariableToValue());
            FunctionRegistry.recordRunCost(p.getName(), overhead + res.getCycles());
            programsRanCount += 1;
        } else {
            debugProgram = p;
            debugInput = new ArrayList<>(input);
            debugExpansionLevel = expansionLevel;
            chargedDebugCycles = res.getCycles();
            currentRunArchitecture = architecture;
        }

        return res;
    }

    public ProgramResult stepOver(){
        Program p = debugProgram;
        if (p == null){
            throw new IllegalStateException("Not in a debug session");
        }
        ProgramResult res = p.stepOverWithBudget(credits);
        // Debit only the newly executed cycles; if step exceeded budget, Program rolled back and cycles won't increase
        int delta = res.getCycles() - chargedDebugCycles;
        if (delta > 0) { credits -= delta; usedCredits += delta; }
        chargedDebugCycles = res.getCycles();
        if(!res.isDebug()){
            Statistic.saveRunDetails(userId, debugExpansionLevel, currentRunArchitecture, debugInput, res.getResult(), res.getCycles(), res.getVariableToValue());
            FunctionRegistry.recordRunCost(p.getName(), currentRunOverhead + res.getCycles());
            programsRanCount += 1;
            debugProgram = null;
            debugInput = null;
            debugExpansionLevel = 0;
            currentRunOverhead = 0;
            chargedDebugCycles = 0;
            currentRunArchitecture = null;
        }

        return res;
    }

    public ProgramResult stepBack(){
        return debugProgram.stepBack();
    }

    public ProgramResult continueDebug(){
        Program p = debugProgram;
        if (p == null){
            throw new IllegalStateException("Not in a debug session");
        }
        ProgramResult res = p.continueDebugWithBudget(credits);
        int delta = res.getCycles() - chargedDebugCycles;
        if (delta > 0) { credits -= delta; usedCredits += delta; }
        chargedDebugCycles = res.getCycles();
        if(!res.isDebug()){
            Statistic.saveRunDetails(userId, debugExpansionLevel, currentRunArchitecture, debugInput, res.getResult(), res.getCycles(), res.getVariableToValue());
            FunctionRegistry.recordRunCost(p.getName(), currentRunOverhead + res.getCycles());
            programsRanCount += 1;
            debugProgram = null;
            debugInput = null;
            debugExpansionLevel = 0;
            currentRunOverhead = 0;
            chargedDebugCycles = 0;
            currentRunArchitecture = null;
        }

        return res;
    }

    public void stopDebug(){
        // If there is an ongoing debug session, snapshot its current state as a completed run
        if (debugProgram != null){
            try {
                ProgramResult snapshot = debugProgram.snapshotDebugAsFinished();
                Statistic.saveRunDetails(userId, debugExpansionLevel, currentRunArchitecture, debugInput, snapshot.getResult(), snapshot.getCycles(), snapshot.getVariableToValue());
            } catch (Exception ignored) {
                // If snapshot fails, still proceed to stop debugging
            }
            debugProgram.stopDebug();
            programsRanCount += 1;
        }
        debugProgram = null;
        debugInput = null;
        debugExpansionLevel = 0;
        chargedDebugCycles = 0;
        currentRunOverhead = 0;
        currentRunArchitecture = null;
    }

    public void setBreakpoint(int index){
        debugProgram.setBreakpoint(index);
    }

    public void removeBreakpoint(int index){
        debugProgram.removeBreakpoint(index);
    }

    public boolean isDebugging(){
        return debugProgram != null;
    }

    // ===== Credits and architecture helpers =====
    private static int architectureRank(String a){
        return switch (a) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            default -> 0;
        };
    }

    private void ensureArchitectureAllowed(Program p, String chosen){
        String required = p.getMaxArchitecture();
        if (architectureRank(chosen) < architectureRank(required)){
            throw new IllegalArgumentException("Chosen architecture is lower than program's minimum: required=" + required + ", chosen=" + chosen);
        }
    }

    private int getArchitectureOverhead(String a){
        Integer cost = Architecture.architectureCosts.get(a);
        if (cost == null){
            throw new IllegalArgumentException("Unknown architecture: " + a);
        }
        return cost;
    }

    public ProgramSummary getProgramSummary(int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }

        return p.getSummary();
    }

    public List<String> getLabels(int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }

        List<String> labels = new ArrayList<>(p.getLabels());
        labels.sort((a, b) -> {
            int na = Integer.parseInt(a.substring(1));
            int nb = Integer.parseInt(b.substring(1));
            return Integer.compare(na, nb);
        });
        return labels;
    }

    public List<String> getVariables(int expansionLevel){
        Program p = curProgram;
        if (expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        List<String> variables = new ArrayList<>(p.getPresentVariables());
        variables.sort((a, b) -> {
            int cmpPrefix = Character.compare(a.charAt(0), b.charAt(0));
            if (cmpPrefix != 0) return cmpPrefix;
            int na = Integer.parseInt(a.substring(1));
            int nb = Integer.parseInt(b.substring(1));
            return Integer.compare(na, nb);
        });
        return variables;
    }

    public List<String> getCommandHistory(int expansionLevel, int index){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        return p.getCommands().get(index).getCommandHistory();
    }

    public static String getProgramOwner(String programName){
        return FunctionRegistry.getOwnerByName(programName);
    }

    public static OptionalDouble getProgramAverageCost(String programName){
        return FunctionRegistry.getAverageCost(programName);
    }

    public static int getCommandCount(String programName){
        return FunctionRegistry.getProgramByName(programName).getCommands().size();
    }

    public static int getProgramRanCount(String programName){
        return FunctionRegistry.getRunCountByName(programName);
    }

    public static int getProgramMaxExpansionLevel(String programName){
        return FunctionRegistry.getProgramByName(programName).getMaxExpansionLevel();
    }

    public static String getFunctionSourceProgram(String functionName){
        return FunctionRegistry.getFunctionSourceProgram(functionName);
    }

    public static List<Statistic> getUserStatistics(String userId){
        return Statistic.getStatistics(userId);
    }

}