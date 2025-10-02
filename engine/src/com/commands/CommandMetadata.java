package com.commands;

import com.dto.ArgFieldDto;
import com.dto.CommandSchemaDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized registry describing required metadata for each supported command.
 * Quotation-related commands are intentionally excluded for this feature.
 */
public final class CommandMetadata {

    /** Simple argument types supported by S-Instruction-Argument values. */
    public enum ArgType {
        STRING,
        INTEGER,
        LABEL
    }

    /** Immutable schema describing a command's inputs. */
    public static final class CommandSchema {
        private final String commandName;
        private final boolean supportsLabel;    // optional S-Label presence
        private final String instructionType;   // "basic" | "synthetic"
        private final Map<String, ArgType> requiredArgsByName; // name → type

        public CommandSchema(
                String commandName,
                boolean supportsLabel,
                String instructionType,
                Map<String, ArgType> requiredArgsByName
        ) {
            this.commandName = commandName;
            this.supportsLabel = supportsLabel;
            this.instructionType = instructionType;
            this.requiredArgsByName = Collections.unmodifiableMap(new LinkedHashMap<>(requiredArgsByName));
        }

        public String getCommandName() {
            return commandName;
        }

        public boolean supportsLabel() {
            return supportsLabel;
        }

        public String getInstructionType() {
            return instructionType;
        }

        public Map<String, ArgType> getRequiredArgsByName() {
            return requiredArgsByName;
        }

        /**
         * Convert this schema to a DTO for UI/API consumption.
         */
        public CommandSchemaDto toDto() {
            List<ArgFieldDto> argDtos = new ArrayList<>();
            for (Map.Entry<String, ArgType> entry : requiredArgsByName.entrySet()) {
                String argName = entry.getKey();
                String displayName = displayNameFor(argName);
                String typeString = entry.getValue().name();
                argDtos.add(new ArgFieldDto(argName, displayName, typeString, true));
            }
            return new CommandSchemaDto(
                    commandName,
                    instructionType,
                    supportsLabel,
                    argDtos
            );
        }
    }

    private static final Map<String, CommandSchema> SCHEMAS;
    static {
        Map<String, CommandSchema> m = new LinkedHashMap<>();

        // BASIC
        m.put("NEUTRAL", new CommandSchema(
                "NEUTRAL", true, "basic",
                Map.of()
        ));

        m.put("INCREASE", new CommandSchema(
                "INCREASE", true, "basic",
                Map.of()
        ));

        m.put("DECREASE", new CommandSchema(
                "DECREASE", true, "basic",
                Map.of()
        ));

        m.put("JUMP_NOT_ZERO", new CommandSchema(
                "JUMP_NOT_ZERO", true, "basic",
                Map.of("JNZLabel", ArgType.LABEL)
        ));

        // SYNTHETIC
        m.put("ZERO_VARIABLE", new CommandSchema(
                "ZERO_VARIABLE", true, "synthetic",
                Map.of()
        ));

        m.put("ASSIGNMENT", new CommandSchema(
                "ASSIGNMENT", true, "synthetic",
                Map.of("assignedVariable", ArgType.STRING)
        ));

        m.put("GOTO_LABEL", new CommandSchema(
                "GOTO_LABEL", true, "synthetic",
                Map.of("gotoLabel", ArgType.LABEL)
        ));

        m.put("CONSTANT_ASSIGNMENT", new CommandSchema(
                "CONSTANT_ASSIGNMENT", true, "synthetic",
                Map.of("constantValue", ArgType.INTEGER)
        ));

        m.put("JUMP_ZERO", new CommandSchema(
                "JUMP_ZERO", true, "synthetic",
                Map.of("JZLabel", ArgType.LABEL)
        ));

        m.put("JUMP_EQUAL_CONSTANT", new CommandSchema(
                "JUMP_EQUAL_CONSTANT", true, "synthetic",
                Map.of(
                        "constantValue", ArgType.INTEGER,
                        "JEConstantLabel", ArgType.LABEL
                )
        ));

        m.put("JUMP_EQUAL_VARIABLE", new CommandSchema(
                "JUMP_EQUAL_VARIABLE", true, "synthetic",
                Map.of(
                        "variableName", ArgType.STRING,
                        "JEVariableLabel", ArgType.LABEL
                )
        ));

        // Quotation-related commands (QUOTE, JUMP_EQUAL_FUNCTION) are excluded by request.

        SCHEMAS = Collections.unmodifiableMap(m);
    }

    private CommandMetadata() {}

    public static Set<String> getSupportedCommandNames() {
        return SCHEMAS.keySet();
    }

    public static boolean isSupported(String commandName) {
        return SCHEMAS.containsKey(commandName);
    }

    public static CommandSchema getSchema(String commandName) {
        CommandSchema schema = SCHEMAS.get(commandName);
        if (schema == null) {
            throw new IllegalArgumentException("Unknown or unsupported command: " + commandName);
        }
        return schema;
    }

    /**
     * Map internal argument keys to user-friendly display names for UI forms.
     */
    private static String displayNameFor(String argKey) {
        return switch (argKey) {
            case "JNZLabel", "JZLabel", "JEConstantLabel", "JEVariableLabel", "gotoLabel" -> "target label";
            case "assignedVariable", "variableName" -> "other variable";
            case "constantValue" -> "value";
            default -> argKey;
        };
    }
}


