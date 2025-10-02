package com.dto;

import java.util.List;

/**
 * Describes a command schema for UI consumption.
 */
public final class CommandSchemaDto {

    private final String commandName;
    private final String instructionType; // "basic" | "synthetic"
    private final boolean supportsLabel;
    private final List<ArgFieldDto> requiredArgs;

    public CommandSchemaDto(
            String commandName,
            String instructionType,
            boolean supportsLabel,
            List<ArgFieldDto> requiredArgs
    ) {
        this.commandName = commandName;
        this.instructionType = instructionType;
        this.supportsLabel = supportsLabel;
        this.requiredArgs = List.copyOf(requiredArgs);
    }

    public String getCommandName() {
        return commandName;
    }

    public String getInstructionType() {
        return instructionType;
    }

    public boolean isSupportsLabel() {
        return supportsLabel;
    }

    public List<ArgFieldDto> getRequiredArgs() {
        return requiredArgs;
    }
}


