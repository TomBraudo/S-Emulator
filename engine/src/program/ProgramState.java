package program;

import java.util.HashMap;
import java.util.List;

/*
    The class is a helper for running full programs
 */
class ProgramState {
    /*
        Assuming the building of the programState singleton object is smart,
        and every variable that is present in the program is entered into this map.
    */
    HashMap<String, Variable> variables;
    List<BaseCommand> commands;
    int currentCommandIndex;
    HashMap<String, Integer> labelToIndex;
    int cyclesCount;
    boolean done;

    ProgramState(List<Integer> input, List<String> presentVariables, List<BaseCommand> commands, HashMap<String, Integer> labelToIndex) {
        variables = new HashMap<>();
        variables.put("y", new Variable("y", 0));
        for (int i = 0; i < input.size(); i++) {
            Variable v = new Variable(String.format("x%d",i+1), input.get(i));
            variables.put(v.getName(), v);
        }
        for(String variableName : presentVariables) {
            if(!variables.containsKey(variableName)) {
                variables.put(variableName, new Variable(variableName, 0));
            }
        }
        this.commands = commands;
        this.currentCommandIndex = 0;
        this.labelToIndex = labelToIndex;
        this.cyclesCount = 0;
        this.done = false;
    }
}

