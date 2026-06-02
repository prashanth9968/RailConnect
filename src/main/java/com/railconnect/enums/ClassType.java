package com.railconnect.enums;
public enum ClassType {
    SL("Sleeper Class"),
    THIRD_AC("Third AC - 3A"),
    SECOND_AC("Second AC - 2A"),
    FIRST_AC("First AC - 1A"),
    AC_CHAIR_CAR("AC Chair Car - CC"),
    EXECUTIVE_CHAIR("Executive Chair - EC"),
    SECOND_SEATING("Second Seating - 2S"),
    FIRST_CLASS("First Class - FC");
    private final String displayName;
    ClassType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
