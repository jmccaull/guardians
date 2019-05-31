package com.walmart.guardians.basics;

/**
 * Simple java enum to match the enum in guardians.graphqls
 */
public enum Stones {
    POWER,
    REALITY,
    MIND,
    SOUL,
    TIME,
    SPACE;


    public String getPower() {
        switch (this) {
            case SPACE:
                return "teleporting";
            case TIME:
                return "timetravel";
                default: return "magical powers";
        }
    }
}
