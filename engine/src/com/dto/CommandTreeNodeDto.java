package com.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandTreeNodeDto implements Serializable {
    private final long id;
    private final List<Integer> path; // root->child indices
    private final String text;
    private final String label;
    private final boolean isBase;
    private final boolean isExpanded;
    private final List<CommandTreeNodeDto> children;

    public CommandTreeNodeDto(long id,
                              List<Integer> path,
                              String text,
                              String label,
                              boolean isBase,
                              boolean isExpanded,
                              List<CommandTreeNodeDto> children) {
        this.id = id;
        this.path = path == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(path));
        this.text = text;
        this.label = label;
        this.isBase = isBase;
        this.isExpanded = isExpanded;
        this.children = children == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(children));
    }

    public long getId() { return id; }
    public List<Integer> getPath() { return path; }
    public String getText() { return text; }
    public String getLabel() { return label; }
    public boolean isBase() { return isBase; }
    public boolean isExpanded() { return isExpanded; }
    public List<CommandTreeNodeDto> getChildren() { return children; }
}


