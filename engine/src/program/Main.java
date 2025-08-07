package program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Integer> input = new ArrayList<Integer>();
        input.add(2);
        List<String> presentVariables = new ArrayList<>();
        presentVariables.add("x1");
        List<BaseCommand> commands = new ArrayList<>();
        commands.add(new Decrease("x1", "L1", 0));
        commands.add(new Increase("y", "-1", 1));
        commands.add(new JumpNotZero("x1", "L1", "-1", 2));
        HashMap<String, Integer> labelToIndex = new HashMap<>();
        labelToIndex.put("L1", 0);
        for(BaseCommand command : commands){
            System.out.println(command.toString());
        }

        ProgramState programState = new ProgramState(input, presentVariables, commands, labelToIndex);
        while (!programState.done && programState.currentCommandIndex < commands.size()) {
            programState.commands.get(programState.currentCommandIndex).execute(programState);
        }
        System.out.printf("Output: %d%n", programState.variables.get("y").getValue());
        System.out.printf("Number of cycles: %d%n", programState.cyclesCount);
    }
}
