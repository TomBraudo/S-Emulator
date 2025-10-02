package com.dto;

/**
 * Describes a single argument field required by a command.
 */
public final class ArgFieldDto {

    private final String name;         // internal key, e.g., "JNZLabel"
    private final String displayName;  // user-friendly name, e.g., "targetLabel"
    private final String type;         // STRING | INTEGER | LABEL
    private final boolean required;    // currently always true; future-proofing

    public ArgFieldDto(String name, String displayName, String type, boolean required) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }
}


