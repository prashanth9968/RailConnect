package com.railconnect.enums;

public enum CoachClass {
    SL("Sleeper Class", false),
    AC3("AC 3-Tier", true),
    AC2("AC 2-Tier", true),
    AC1("AC First Class", true),
    CC("Chair Car", false),
    EC("Executive Chair Car", true),
    SECOND("Second Sitting", false),
    THIRD_AC_ECONOMY("Third AC Economy", true);

    private final String displayName;
    private final boolean isAC;

    CoachClass(String displayName, boolean isAC) {
        this.displayName = displayName;
        this.isAC = isAC;
    }

    public String getDisplayName() { return displayName; }
    public boolean isAC() { return isAC; }
}
