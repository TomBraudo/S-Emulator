package program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Integer> input = new ArrayList<Integer>();
        input.add(10);
        List<BaseCommand> commands = new ArrayList<>();
        commands.add(new Decrease("x1", "L1", 0));
        commands.add(new Increase("y", "-1", 1));
        commands.add(new JumpNotZero("x1", "L1", "-1", 2));
        Program p = new Program("Identity function", commands);
        System.out.println(p.toString());

        ProgramState result = p.execute(input);
        System.out.println("Output: " + result.variables.get("y").getValue());
        System.out.println("Number of cycles: " + result.cyclesCount);
    }
}
