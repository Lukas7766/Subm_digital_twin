package com.digitwin.digitaltwin.model;

public enum JobStatus {
    STARTED,
    PAUSED,
    RESUMED,
    CANCELLED,
    FAILED,
    FINISHED;

    public boolean isActive(){
        return this == JobStatus.STARTED || this == RESUMED || this == JobStatus.PAUSED;
    }

    public boolean isTerminal(){
        return this == JobStatus.FINISHED || this == CANCELLED || this == JobStatus.FAILED;
    }
}