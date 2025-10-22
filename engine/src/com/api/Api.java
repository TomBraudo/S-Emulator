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
import com.dto.api.*;
import com.program.MixedExpansionSession;
import com.program.Program;
import com.program.Architecture;
import com.program.FunctionRegistry;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Api {
    private Program curProgram;
    private Program debugProgram;
    private List<Integer> debugInput;
    private int debugExpansionLevel;
    // Mixed tree view state (visual-only)
    private MixedExpansionSession mixedSession;
    private final String userId;
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
        double avg = FunctionRegistry.getAverageCost(p.getName());
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
        // Save statistics and global averages (cycles only, not overhead)
        boolean isFunction = FunctionRegistry.isFunction(p.getName());
        Statistic.saveRunDetails(userId, p.getName(), isFunction, expansionLevel, architecture, input, res.getResult(), res.getCycles(), res.getVariableToValue());
        FunctionRegistry.recordRunCost(p.getName(), res.getCycles());
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

    public ProgramCommands getProgramCommands(int expansionLevel){
        Program p = curProgram.expand(expansionLevel);
        List<String> commands = p.getCommands().stream().map(BaseCommand::toString).toList();
        List<String> architectures = p.getCommands().stream().map(BaseCommand::getArchitecture).toList();
        return new ProgramCommands(commands, architectures);
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
        double avg = FunctionRegistry.getAverageCost(p.getName());
        if (avg + overhead > credits){
            throw new IllegalStateException("Insufficient credits: average run cost plus overhead exceeds available credits");
        }
        // Charge overhead and start debug with budget
        credits -= overhead;
        usedCredits += overhead;
        currentRunOverhead = overhead;
        ProgramResult res = p.startDebugWithBudget(input, breakpoints, credits);
        
        // Wrap result with sessionCycles (initial cycles = total cycles since starting from 0)
        int sessionCycles = res.getCycles();
        HashMap<String, Integer> vars = new HashMap<>();
        for (ProgramResult.VariableToValue vtv : res.getVariableToValue()) {
            vars.put(vtv.variable(), vtv.value());
        }
        ProgramResult wrappedRes = new ProgramResult(res.getCycles(), sessionCycles, vars, res.getDebugIndex(), res.isDebug(), res.getHaltReason());
        
        // Debit cycles executed up to initial breakpoint/end
        if (wrappedRes.getCycles() > 0){
            credits -= wrappedRes.getCycles();
            usedCredits += wrappedRes.getCycles();
        }
        if(!wrappedRes.isDebug()){
            // finished immediately
            boolean isFunction = FunctionRegistry.isFunction(p.getName());
            Statistic.saveRunDetails(userId, p.getName(), isFunction, expansionLevel, architecture, input, wrappedRes.getResult(), wrappedRes.getCycles(), wrappedRes.getVariableToValue());
            FunctionRegistry.recordRunCost(p.getName(), wrappedRes.getCycles());
            programsRanCount += 1;
        } else {
            debugProgram = p;
            debugInput = new ArrayList<>(input);
            debugExpansionLevel = expansionLevel;
            chargedDebugCycles = wrappedRes.getCycles();
            currentRunArchitecture = architecture;
        }

        return wrappedRes;
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
        
        // Wrap result with sessionCycles = delta
        HashMap<String, Integer> vars = new HashMap<>();
        for (ProgramResult.VariableToValue vtv : res.getVariableToValue()) {
            vars.put(vtv.variable(), vtv.value());
        }
        ProgramResult wrappedRes = new ProgramResult(res.getCycles(), delta, vars, res.getDebugIndex(), res.isDebug(), res.getHaltReason());
        
        if(!wrappedRes.isDebug()){
            boolean isFunction = FunctionRegistry.isFunction(p.getName());
            Statistic.saveRunDetails(userId, p.getName(), isFunction, debugExpansionLevel, currentRunArchitecture, debugInput, wrappedRes.getResult(), wrappedRes.getCycles(), wrappedRes.getVariableToValue());
            FunctionRegistry.recordRunCost(p.getName(), wrappedRes.getCycles());
            programsRanCount += 1;
            debugProgram = null;
            debugInput = null;
            debugExpansionLevel = 0;
            currentRunOverhead = 0;
            chargedDebugCycles = 0;
            currentRunArchitecture = null;
        }

        return wrappedRes;
    }

    public ProgramResult stepBack(){
        Program p = debugProgram;
        if (p == null){
            throw new IllegalStateException("Not in a debug session");
        }
        int previousCycles = chargedDebugCycles;
        ProgramResult res = p.stepBack();
        // Calculate delta (cycles decreased, but we still CHARGE for stepping back)
        int delta = Math.abs(res.getCycles() - previousCycles);
        // CHARGE credits for stepping back (not refund)
        credits -= delta;
        usedCredits += delta;
        chargedDebugCycles = res.getCycles();
        
        // Wrap result with sessionCycles = delta
        HashMap<String, Integer> vars = new HashMap<>();
        for (ProgramResult.VariableToValue vtv : res.getVariableToValue()) {
            vars.put(vtv.variable(), vtv.value());
        }
        ProgramResult wrappedRes = new ProgramResult(res.getCycles(), delta, vars, res.getDebugIndex(), res.isDebug(), res.getHaltReason());
        
        return wrappedRes;
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
        
        // Wrap result with sessionCycles = delta
        HashMap<String, Integer> vars = new HashMap<>();
        for (ProgramResult.VariableToValue vtv : res.getVariableToValue()) {
            vars.put(vtv.variable(), vtv.value());
        }
        ProgramResult wrappedRes = new ProgramResult(res.getCycles(), delta, vars, res.getDebugIndex(), res.isDebug(), res.getHaltReason());
        
        if(!wrappedRes.isDebug()){
            boolean isFunction = FunctionRegistry.isFunction(p.getName());
            Statistic.saveRunDetails(userId, p.getName(), isFunction, debugExpansionLevel, currentRunArchitecture, debugInput, wrappedRes.getResult(), wrappedRes.getCycles(), wrappedRes.getVariableToValue());
            FunctionRegistry.recordRunCost(p.getName(), wrappedRes.getCycles());
            programsRanCount += 1;
            debugProgram = null;
            debugInput = null;
            debugExpansionLevel = 0;
            currentRunOverhead = 0;
            chargedDebugCycles = 0;
            currentRunArchitecture = null;
        }

        return wrappedRes;
    }

    public int stopDebug(){
        int totalCost = 0;
        // If there is an ongoing debug session, snapshot its current state as a completed run
        if (debugProgram != null){
            // Calculate total cost (overhead + cycles already charged)
            totalCost = currentRunOverhead + chargedDebugCycles;
            
            try {
                ProgramResult snapshot = debugProgram.snapshotDebugAsFinished();
                boolean isFunction = FunctionRegistry.isFunction(debugProgram.getName());
                Statistic.saveRunDetails(userId, debugProgram.getName(), isFunction, debugExpansionLevel, currentRunArchitecture, debugInput, snapshot.getResult(), snapshot.getCycles(), snapshot.getVariableToValue());
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
        
        return totalCost;
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

    public static double getProgramAverageCost(String programName){
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

    public static List<String> getProgramNames(){
        return FunctionRegistry.getProgramNames();
    }

    public static List<String> getFunctionNames(){
        return FunctionRegistry.getFunctionNames();
    }

    public static List<String> getAllFunctionsInChain(String name){
        return FunctionRegistry.getAllFunctionsInChain(name);
    }

    public static List<String> getProgramsUsing(String functionName){
        return FunctionRegistry.getProgramsUsing(functionName);
    }

    public static ProgramInfo getProgramInformation(String programName){
        String owner = getProgramOwner(programName);
        int commandsCount = getCommandCount(programName);
        int maxLevel = getProgramMaxExpansionLevel(programName);
        int ranCount = getProgramRanCount(programName);
        double averageCost = getProgramAverageCost(programName);
        boolean isFunction = FunctionRegistry.isFunction(programName);
        String source = isFunction ? getFunctionSourceProgram(programName) : null;
        return new ProgramInfo(programName, owner, commandsCount, maxLevel, ranCount, averageCost, source, isFunction);
    }

    public static List<Statistic> getUserStatistics(String userId){
        return Statistic.getStatistics(userId);
    }

    public UserInfo getInfo(){
        return new UserInfo(
                userId,
                FunctionRegistry.getProgramUploadedCount(userId),
                FunctionRegistry.getFunctionUploadedCount(userId),
                credits,
                usedCredits,
                getUserStatistics(userId).size()
        );
    }

    public Map.Entry<Boolean, String> canRun(int expansionLevel, String architecture){
        //part 1, verifies average cost against available credits
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        double neededCredits = getProgramAverageCost(p.getName()) + getArchitectureOverhead(architecture);
        if (neededCredits > credits){
            return new AbstractMap.SimpleEntry<>(false, "Insufficient credits");
        }

        //part 2, verifies all commands are allowed
        try {
            for (BaseCommand cmd : p.getCommands()) {
                String cmdArch = cmd.getArchitecture();
                if (architectureRank(architecture) < architectureRank(cmdArch)) {
                    return new AbstractMap.SimpleEntry<>(false, "Chosen architecture is lower than command's minimum: required=" + cmdArch + ", chosen=" + architecture);
                }
            }
        }
        catch (IllegalArgumentException e){
            return new AbstractMap.SimpleEntry<>(false, "Program contains unknown command: " + e.getMessage());
        }

        return new AbstractMap.SimpleEntry<>(true, null);
    }

}