package com.bxdlove.worktimeserver.api;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.bxdlove.worktimeserver.startup.setup.SetupException;

import java.util.Stack;

@ApplicationScoped
@Path("/status")
public class Status {

    private boolean isRunning;
    private Stack<SetupException> exceptions;

    @PostConstruct
    public void init() {
        isRunning = false;
        exceptions = new Stack<>();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public int getExceptionCount() {
        return exceptions.size();
    }

    public void addException(SetupException exception) {
        exceptions.push(exception);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() {
        return Response.status(isRunning ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE).build();
    }
}
