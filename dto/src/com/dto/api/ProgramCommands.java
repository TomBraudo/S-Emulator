package com.dto.api;

import java.util.List;

public class ProgramCommands {
    List<String> commands;
    List<String> architectures;
    public ProgramCommands(List<String> commands, List<String> architectures) {
        this.commands = commands;
        this.architectures = architectures;
    }
    public List<String> getCommands() {
        return commands;
    }
    public List<String> getArchitectures() {
        return architectures;
    }
}
