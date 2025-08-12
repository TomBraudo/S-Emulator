package com.api;
import com.XMLHandler.SInstruction;
import com.XMLHandler.SProgram;
import com.commands.BaseCommand;
import com.commands.CommandFactory;
import com.program.Program;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Api {
    private static Program curProgram;

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

    public static List<String> getInputVariableNames(){
        return curProgram.getInputVariables();
    }

}

