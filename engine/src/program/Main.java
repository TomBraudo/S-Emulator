package program;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            //Api.loadSProgram("C:\\Users\\Tom\\OneDrive\\Desktop\\EX 1\\error-1.xml");
            List<BaseCommand> commands = new ArrayList<>();
            commands.add(new Assignment("y", "x1",  BaseCommand.NO_LABEL,0, null));
            Program p = new Program("Id", commands);
            Program p2 = p.expand(1);
            Program p3 = p.expand(2);
            System.out.println(p.execute(java.util.List.of(10)).getResult());
            System.out.println("-----------------------------------");
            System.out.println(p2.execute(java.util.List.of(10)).getResult());
            System.out.println("-----------------------------------");
            System.out.println(p3.execute(java.util.List.of(10)).getResult());

        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Stack Trace:" + Arrays.toString(e.getStackTrace()));
        }



    }
}
