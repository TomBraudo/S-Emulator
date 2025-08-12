package menu.ui;

import com.api.Api;
import com.api.ProgramResult;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ExecuteProgramItem implements MenuItem{

    @Override
    public String getTitle() {
        return "Execute Program";
    }

    @Override
    public void onSelect() {
        if(!MenuUtils.ensureProgramLoaded()) return;

        int level = MenuUtils.getLevelInput();
        if(level == -1) return;


        System.out.println("The input variables are:");
        System.out.println(Api.getInputVariableNames());

        List<Integer> programInput = getInputVariables();
        if(programInput == null) return;

        printRes(programInput, level);

    }

    private List<Integer> getInputVariables(){

        List<Integer> programInput = List.of();
        while (true) {
            System.out.println("Enter the input as a list of numbers seperated by a comma (or '-1' to return):");
            System.out.println("Example: '1,2,3' will be translated to: x1=1, x2=2, x3=3");

            String input = MenuUtils.SCANNER.nextLine().trim();

            if(input.equals("-1")) {
                return null;
            }

            if(input.matches("\\d+(\\s*,\\s*\\d+)*")){
                programInput = Arrays.stream(input.split("\\s*,\\s*"))
                        .map(Integer::parseInt)
                        .toList();
                break;
            }
            else {
                System.out.println("Invalid format. Try again.\n");
            }
        }

        return programInput;
    }

    private void printRes(List<Integer> programInput, int level){
        ProgramResult res = Api.executeProgram(programInput,level);
        System.out.println("The program that has been executed:");
        System.out.println("===================================");
        System.out.println(Api.getProgram(level));
        System.out.println("===================================");
        System.out.println("Result: " + res.getResult());
        System.out.println("Final variables values: ");
        for(ProgramResult.VariableToValue variableToValue : res.getVariableToValue()) {
            System.out.printf("%s=%d%n", variableToValue.variable(), variableToValue.value());
        }

        System.out.println("===================================");
        System.out.println("The number of cycles the program took: " + res.getCycles());
    }
}
