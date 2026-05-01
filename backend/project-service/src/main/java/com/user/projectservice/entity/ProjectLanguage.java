package com.user.projectservice.entity;

public enum ProjectLanguage {
    JAVA("Java"),
    NODEJS("Node.js"),
    PYTHON("Python"),
    GO("Go"),
    PHP("PHP"),
    UNKNOWN("Générique");

    private final String displayName;

    ProjectLanguage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
