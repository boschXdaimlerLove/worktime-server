package com.bxdlove.worktimeserver.api.json;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class TimeObject {

    @NotNull
    @JsonbProperty("start")
    private OffsetDateTime start;

    @NotNull
    @JsonbProperty("end")
    @JsonbDateFormat
    private OffsetDateTime end;

    public TimeObject() {
    }

    @SuppressWarnings("unused")
    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    @SuppressWarnings("unused")
    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }
}
