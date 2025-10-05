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
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Map;

public class Api {
    private Program curProgram;
    private Program debugProgram;
    private List<Integer> debugInput;
    private int debugExpansionLevel;
    // Mixed tree view state (visual-only)
    private MixedExpansionSession mixedSession;
    private String userId;
    private int credits;
    
    public Api(String userId){
        this.userId = userId;
    }

    public String getCurProgramName() {
        return curProgram.getName();
    }

    public void loadSProgram(InputStream xmlStream) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(SProgram.class);
        Unmarshaller um = ctx.createUnmarshaller();
        SProgram sp = ((SProgram) um.unmarshal(xmlStream));
        // Pre-validate program name uniqueness prior to any registration
        FnArgs.assertProgramNameAvailable(this.userId, sp.getName());
        // Phase 1: register function names/arity atomically; will throw without partial writes
        CommandFactory.registerFunctions(this.userId, sp.getSFunctions());
        // Build and register program only after successful validation/registration
        curProgram = Program.createProgram(sp.getName(), sp.getSInstructions().getSInstruction());
        FnArgs.registerProgram(this.userId, curProgram);
        Statistic.clearStatistics();
    }
    public void createEmptyProgram(String name){
        curProgram = Program.createProgram(name, Collections.emptyList());
        FnArgs.registerProgram(this.userId, curProgram);
        Statistic.clearStatistics();
    }


    public ProgramResult executeProgram(List<Integer> input, int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }

        ProgramResult res = p.execute(input);
        Statistic.saveRunDetails(expansionLevel, input, res.getResult(), res.getCycles(), res.getVariableToValue());
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

    public void saveState(String path){
        if(curProgram == null){
            throw new IllegalArgumentException("No program is loaded.");
        }

        Path folder = Paths.get(path);
        if(!Files.exists(folder) ||  !Files.isDirectory(folder)){
            throw new IllegalArgumentException("The path provided is not a directory.\n" + path);
        }

        String programName = curProgram.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = programName + "_" + timestamp + ".dat";

        Path saveFile = folder.resolve(fileName);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile.toFile()))) {
            out.writeObject(new FullSystemState(curProgram));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save state to folder: " + path, e);
        }

    }

    public static void loadState(String path){
        if(!path.endsWith(".dat")){
            throw new IllegalArgumentException("The path provided is not a valid state file.\n" + path);
        }

        Path file = Paths.get(path);
        if(!Files.exists(file) ||  !Files.isRegularFile(file)){
            throw new IllegalArgumentException("The path provided is not a file.\n" + path);
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path))){
            ((FullSystemState) in.readObject()).apply();
        }
        catch (IOException | ClassNotFoundException e){
            throw new RuntimeException("Failed to load state from " + path, e);
        }
    }

    public ProgramResult startDebugging(List<Integer> input, int expansionLevel, List<Integer> breakpoints){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }

        ProgramResult res = p.startDebug(input, breakpoints);
        if(!res.isDebug()){
            Statistic.saveRunDetails(expansionLevel, input, res.getResult(), res.getCycles(), res.getVariableToValue());
        }
        else{
            debugProgram = p;
            debugInput = new ArrayList<>(input);
            debugExpansionLevel = expansionLevel;
        }

        return res;
    }

    public ProgramResult stepOver(){
        Program p = debugProgram;
        ProgramResult res = p.stepOver();
        if(!res.isDebug()){
            Statistic.saveRunDetails(debugExpansionLevel, debugInput, res.getResult(), res.getCycles(), res.getVariableToValue());
            debugProgram = null;
            debugInput = null;
            debugExpansionLevel = 0;
        }

        return res;
    }

    public ProgramResult stepBack(){
        return debugProgram.stepBack();
    }

    public ProgramResult continueDebug(){
        Program p = debugProgram;
        ProgramResult res = p.continueDebug();
        if(!res.isDebug()){
            Statistic.saveRunDetails(debugExpansionLevel, debugInput, res.getResult(), res.getCycles(), res.getVariableToValue());
            debugProgram = null;
            debugInput = null;
            debugExpansionLevel = 0;
        }

        return res;
    }

    public void stopDebug(){
        // If there is an ongoing debug session, snapshot its current state as a completed run
        if (debugProgram != null){
            try {
                ProgramResult snapshot = debugProgram.snapshotDebugAsFinished();
                Statistic.saveRunDetails(debugExpansionLevel, debugInput, snapshot.getResult(), snapshot.getCycles(), snapshot.getVariableToValue());
            } catch (Exception ignored) {
                // If snapshot fails, still proceed to stop debugging
            }
            debugProgram.stopDebug();
        }
        debugProgram = null;
        debugInput = null;
        debugExpansionLevel = 0;
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
}