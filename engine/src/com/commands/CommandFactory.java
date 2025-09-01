package com.commands;
import com.XMLHandlerV2.SInstruction;
import com.XMLHandlerV2.SInstructionArgument;
import com.program.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandFactory {

    private static final Map<String, List<com.XMLHandlerV2.SInstruction>> FUNCTIONS = new HashMap<>();

    public static void registerFunctions(com.XMLHandlerV2.SFunctions functions) {
        FUNCTIONS.clear();
        if (functions == null) return;
        for (com.XMLHandlerV2.SFunction f : functions.getSFunction()) {
            FUNCTIONS.put(f.getName(), f.getSInstructions().getSInstruction());
        }
    }

    private static List<String> parseFunctionArgs(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        String[] parts = csv.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }



    private static Map<String, String> mapArgs(List<SInstructionArgument> argsList) {
        Map<String, String> argsMap = new HashMap<>();
        if (argsList != null) {
            for (SInstructionArgument arg : argsList) {
                argsMap.put(arg.getName(), arg.getValue());
            }
        }
        return argsMap;
    }

    public static BaseCommand createCommand(
            String name,
            String variable,                 // SInstruction.sVariable
            String label,                    // SInstruction.sLabel, may be null
            List<SInstructionArgument> args, // SInstructionArguments, may be null
            int index                        // position in program
    ) {
        String safeLabel = (label != null) ? label : BaseCommand.NO_LABEL;
        Map<String, String> argMap = mapArgs(args);

        return switch (name) {

            // ===== BASIC COMMANDS =====
            case "DECREASE" -> new Decrease(variable, safeLabel, index, null);
            case "INCREASE" -> new Increase(variable, safeLabel, index, null);
            case "JUMP_NOT_ZERO" ->
                    new JumpNotZero(
                            variable,
                            argMap.get("JNZLabel"), // label to jump to
                            safeLabel,
                            index,
                            null
                    );
            case "NEUTRAL" -> new Neutral(variable, safeLabel, index, null);

            // ===== SYNTHETIC COMMANDS =====

            case "ZERO_VARIABLE" ->
                    new ZeroVariable(variable, safeLabel, index, null);

            case "ASSIGNMENT" ->
                    new Assignment(
                            variable,
                            argMap.get("assignedVariable"), // variable to copy from
                            safeLabel,
                            index,
                            null
                    );

            case "CONSTANT_ASSIGNMENT" ->
                    new ConstantAssignment(
                            variable,
                            Integer.parseInt(argMap.get("constantValue")),
                            safeLabel,
                            index,
                            null
                    );

            case "GOTO_LABEL" ->
                    new GotoLabel(
                            argMap.get("gotoLabel"), // label to jump to
                            safeLabel,
                            index,
                            null
                    );

            case "JUMP_ZERO" ->
                    new JumpZero(
                            variable,
                            argMap.get("JZLabel"), // label to jump to
                            safeLabel,
                            index,
                            null
                    );

            case "JUMP_EQUAL_CONSTANT" ->
                    new JumpEqualConstant(
                            variable,
                            Integer.parseInt(argMap.get("constantValue")),
                            argMap.get("JEConstantLabel"), // target label
                            safeLabel,
                            index,
                            null
                    );

            case "JUMP_EQUAL_VARIABLE" ->
                    new JumpEqualVariable(
                            variable,
                            argMap.get("variableName"),     // other variable name
                            argMap.get("JEVariableLabel"),  // target label
                            safeLabel,
                            index,
                            null
                    );
            case "QUOTE" -> {
                Program p = getProgramFromArg(argMap);
                List<String> fnArgs = parseFunctionArgs(argMap.get("functionArguments"));
                yield new Quotation(variable, p, fnArgs, safeLabel, index, null);
            }

            case "JUMP_EQUAL_FUNCTION" -> {
                Program p = getProgramFromArg(argMap);
                List<String> fnArgs = parseFunctionArgs(argMap.get("functionArguments"));
                String targetLabel = argMap.get("JEFunctionLabel");
                yield new JumpEqualFunction(variable, targetLabel, p, fnArgs, safeLabel, index, null);
            }

            default -> throw new IllegalArgumentException("Unknown command name: " + name);
        };
    }

    private static Program getProgramFromArg(Map<String, String> argMap) {
        String functionName = argMap.get("functionName");
        if (functionName == null) throw new IllegalArgumentException("Missing function name");
        List<SInstruction> functionInstructions = FUNCTIONS.get(functionName);
        if (functionInstructions == null) throw new IllegalArgumentException("QUOTE function not found: " + functionName);
        return Program.createProgram(functionName, functionInstructions);
    }
}
