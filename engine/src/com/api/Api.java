package com.api;
import com.XMLHandlerV2.SProgram;
import com.commands.BaseCommand;
import com.commands.CommandFactory;
import com.commands.FnArgs;
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

public class Api {
    private static Program curProgram;
    private static Program debugProgram;
    private static List<Integer> debugInput;
    private static int debugExpansionLevel;

    public static String getCurProgramName() {
        return curProgram.getName();
    }

    public static void loadSProgram(String path) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(SProgram.class);
        Unmarshaller um = ctx.createUnmarshaller();
        SProgram sp = ((SProgram) um.unmarshal(new File(path)));
        CommandFactory.registerFunctions(sp.getSFunctions());
        curProgram = Program.createProgram(sp.getName(), sp.getSInstructions().getSInstruction());
        FnArgs.registerProgram(curProgram.getName(), curProgram);
        Statistic.clearStatistics();
    }


    public static ProgramResult executeProgram(List<Integer> input, int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }

        ProgramResult res = p.execute(input);
        Statistic.saveRunDetails(expansionLevel, input, res.getResult(), res.getCycles(), res.getVariableToValue());
        return res;
    }

    public static String getProgram(int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        return p.toString();
    }

    public static boolean isLoaded(){
        return curProgram != null;
    }

    public static int getMaxLevel(){
        return curProgram.getMaxExpansionLevel();
    }

    static void setCurProgram(Program curProgram){
        Api.curProgram = curProgram;
    }

    public static List<String> getInputVariableNames(){
        return curProgram.getInputVariables();
    }

    public static List<String> getProgramCommands(int expansionLevel){
        return curProgram.expand(expansionLevel).getCommands().stream().map(BaseCommand::toString).toList();
    }

    public static List<String> getAvailableFunctions(){
        return FnArgs.getFunctionNames();
    }

    public static List<String> getFunctionCommands(String functionName, int expansionLevel){
        Program p = FnArgs.getProgramByName(functionName).expand(expansionLevel);
        return p.getCommands().stream().map(BaseCommand::toString).toList();
    }

    public static void setCurProgram(String functionName){
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

    public static void saveState(String path){
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

    public static ProgramResult startDebugging(List<Integer> input, int expansionLevel, List<Integer> breakpoints){
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

    public static ProgramResult stepOver(){
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

    public static ProgramResult continueDebug(){
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

    public static void stopDebug(){
        debugProgram.stopDebug();
        debugProgram = null;
        debugInput = null;
        debugExpansionLevel = 0;
    }

    public static void setBreakpoint(int index){
        debugProgram.setBreakpoint(index);
    }

    public static void removeBreakpoint(int index){
        debugProgram.removeBreakpoint(index);
    }

    public static boolean isDebugging(){
        return debugProgram != null;
    }

    public static ProgramSummary getProgramSummary(int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }

        return p.getSummary();
    }

    public static List<String> getLabels(int expansionLevel){
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

    public static List<String> getVariables(int expansionLevel){
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

    public static List<String> getCommandHistory(int expansionLevel, int index){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }
        return p.getCommands().get(index).getCommandHistory();
    }
}