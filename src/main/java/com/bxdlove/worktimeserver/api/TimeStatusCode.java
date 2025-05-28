package com.bxdlove.worktimeserver.api;

public enum TimeStatusCode {
    OK(0),
    TIME_EXCEEDED(1),
    BREAK_NEEDED(2),
    HOLIDAY(3),
    OUTSIDE_CORE_WORKING_HOURS(4),
    NOT_LOGGED_IN(5);

    private final int returnCode;

    TimeStatusCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
