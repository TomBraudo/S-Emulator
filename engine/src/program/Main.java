package program;

import org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.List;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Api.loadSProgram("C:\\Users\\Tom\\OneDrive\\Desktop\\EX 1\\synthetic.xml");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        ProgramResult res = Api.ExecuteProgram(new ArrayList<>(java.util.List.of(15, 2, 3)), 1);
        System.out.println("The program:");
        System.out.println(Api.GetProgram(0));
        System.out.println("Result: " + res.getResult());
        for(AbstractMap.SimpleEntry<String, Integer> entry : res.getVariableToValue()){
            System.out.printf("%s=%d%n", entry.getKey(), entry.getValue());
        }
        System.out.printf("Number of steps: %d", res.getCycles());

    }
}
