package com.simiyami.loveletter.enums;

public enum PlayerType {
    HUMAN("사용자"),
    CPU("CPU");

    private final String displayName;

    PlayerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}