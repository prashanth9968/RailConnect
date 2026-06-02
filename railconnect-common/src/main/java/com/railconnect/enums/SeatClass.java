package com.railconnect.enums;
public enum SeatClass {
    SL("Sleeper"), S3("AC 3 Tier"), S2("AC 2 Tier"), S1("AC First Class"),
    CC("Chair Car"), EC("Executive Chair Car"), GN("General");
    private final String displayName;
    SeatClass(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
