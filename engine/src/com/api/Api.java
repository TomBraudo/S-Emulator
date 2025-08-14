package com.api;
import com.XMLHandler.SInstruction;
import com.XMLHandler.SProgram;
import com.commands.BaseCommand;
import com.commands.CommandFactory;
import com.program.Program;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Api {
    private static Program curProgram;

    public static String getCurProgramName() {
        return curProgram.getName();
    }

    public static void loadSProgram(String path) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(SProgram.class);
        Unmarshaller um = ctx.createUnmarshaller();
        createProgramFromSProgram((SProgram) um.unmarshal(new File(path)));
    }

    private static void createProgramFromSProgram(SProgram sp){
        String name = sp.getName();
        List<BaseCommand> commands = new ArrayList<BaseCommand>();
        List<SInstruction> instructions = sp.getSInstructions().getSInstruction();
        for(int i = 0; i < instructions.size(); i++){
            SInstruction instruction = instructions.get(i);
            commands.add(CommandFactory.createCommand(
                    instruction.getName(),
                    instruction.getSVariable(),
                    instruction.getSLabel(),
                    instruction.getSInstructionArguments() == null
                            ? null :
                            instruction.getSInstructionArguments().getSInstructionArgument(),
                    i));
        }
        curProgram = new Program(name, commands);
        curProgram.verifyLegal();
    }

    public static ProgramResult executeProgram(List<Integer> input, int expansionLevel){
        Program p = curProgram;
        if(expansionLevel > 0){
            p = curProgram.expand(expansionLevel);
        }

        ProgramResult res = p.execute(input);
        Statistic.saveRunDetails(expansionLevel, input, res.getResult(), res.getCycles());
        return res;
    }

    public static void expandProgram(int expansionLevel){
        curProgram = curProgram.expand(expansionLevel);
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
}

