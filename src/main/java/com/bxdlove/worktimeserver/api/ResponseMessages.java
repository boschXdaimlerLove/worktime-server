package com.bxdlove.worktimeserver.api;

import jakarta.ws.rs.core.Response;

public enum ResponseMessages {
    UNAUTHORIZED(Response.status(Response.Status.UNAUTHORIZED).entity("Unauthorized")),
    DATABASE_ERROR(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Internal server error")),
    ALREADY_CHECKED_IN(Response.status(Response.Status.BAD_REQUEST).entity("Already checked in")),
    NO_ACTIVE_TIME_FRAME(Response.status(Response.Status.BAD_REQUEST).entity("No active time frame. Logging out not available."));
    private final Response.ResponseBuilder response;

    ResponseMessages(Response.ResponseBuilder response) {
        this.response = response;
    }

    public Response.ResponseBuilder getResponseBuilder() {
        return response;
    }
}
