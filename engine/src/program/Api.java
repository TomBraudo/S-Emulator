package program;
import XMLHandler.*;
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
    }

    public static ProgramResult ExecuteProgram(List<Integer> input, int expansionLevel){
        return curProgram.execute(input);
    }

    public static String GetProgram(int expansionLevel){
        return curProgram.toString();
    }

}

