package com.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgramTreeDto implements Serializable {
    private final String programName;
    private final List<CommandTreeNodeDto> roots;

    public ProgramTreeDto(String programName, List<CommandTreeNodeDto> roots) {
        this.programName = programName;
        this.roots = roots == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(roots));
    }

    public String getProgramName() { return programName; }
    public List<CommandTreeNodeDto> getRoots() { return roots; }
}


