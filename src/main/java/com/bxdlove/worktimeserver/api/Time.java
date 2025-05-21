package com.bxdlove.worktimeserver.api;

import com.bxdlove.worktimeserver.api.json.TimeObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.RequestScoped;
import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * This class handles employee time entries.
 * It provides the following endpoints:
 * <ol>
 *     <li>POST /time/{employee_mail} - Stamp in for an employee</li>
 *     <li>GET /time/{employee_mail}?range_start={range_start}&range_end={range_end} - Get time entries for an employee between two dates</li>
 *     <li>PATCH /time/{employee_mail} - Stamp out for an employee</li>
 * </ol>
 */
@BasicAuthenticationMechanismDefinition(
        realmName = "worktime"
)
@Path("/time")
@RequestScoped
public class Time {

    /**
     * Stamp in for an employee.
     * This endpoint creates a new time entry for the employee with the given email address.
     *
     * @return Response indicating the result of the operation:
     *  <ul>
     *      <li>{@link ResponseMessages#UNAUTHORIZED}: Wrong login credentials</li>
     *      <li>{@link ResponseMessages#DATABASE_ERROR}: Error with the database</li>
     *      <li>{@link ResponseMessages#ALREADY_CHECKED_IN}: User already checked in before</li>
     *  </ul>
     */
    @POST
    @Path("/stamp-in")
    public Response stampIn(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return ResponseMessages.UNAUTHORIZED.getResponseBuilder().build();
        }

        try (MongoClient mongoclient = MongoClients.create("mongodb://localhost:27017")) {
            if (userAlreadyCheckedIn(mongoclient, securityContext.getUserPrincipal().getName())) {
                return ResponseMessages.ALREADY_CHECKED_IN.getResponseBuilder().build();
            }

            mongoclient.getDatabase("worktime_server").getCollection("time_frame").insertOne(
                    new Document("employee_mail", securityContext.getUserPrincipal().getName())
                            .append("start", LocalDateTime.now())
                            .append("status", "open")
            );
        } catch (MongoException e) {
            return ResponseMessages.DATABASE_ERROR.getResponseBuilder().build();
        }

        return Response.ok().build();
    }

    /**
     * Stamp out for an employee.
     * This endpoint updates the time entry for the employee with the given email address.
     *
     * @return Response indicating the result of the operation:
     *  <ul>
     *      <li>{@link ResponseMessages#UNAUTHORIZED}: Wrong login credentials</li>
     *      <li>{@link ResponseMessages#DATABASE_ERROR}: Error with the database</li>
     *      <li>{@link ResponseMessages#NO_ACTIVE_TIME_FRAME}: User is not stamped in</li>
     *  </ul>
     */
    @POST
    @Path("/stamp-out")
    public Response stampOut(
            @Context SecurityContext securityContext
    ) {
        if (securityContext.getUserPrincipal() == null) {
            return ResponseMessages.UNAUTHORIZED.getResponseBuilder().build();
        }

        try (MongoClient mongoclient = MongoClients.create("mongodb://localhost:27017")) {
            if (getUserTimestampEntry(mongoclient, securityContext.getUserPrincipal().getName()) == null) {
                return ResponseMessages.NO_ACTIVE_TIME_FRAME.getResponseBuilder().build();
            }

            mongoclient.getDatabase("worktime_server").getCollection("time_frame").updateOne(
                    new Document("employee_mail", securityContext.getUserPrincipal().getName()).append("_id", getUserTimestampEntry(mongoclient, securityContext.getUserPrincipal().getName()).get("_id")),
                    new Document("$set", new Document("end", LocalDateTime.now()))
            );
        } catch (MongoException e) {
            return ResponseMessages.DATABASE_ERROR.getResponseBuilder().build();
        }

        return Response.ok().build();
    }

    /**
     * Creates a new time frame of the authenticated user.
     *
     * @param securityContext provides security-related context
     * @param timeObjects contains the start and time of
     * @return a response indicating the result of the operation:
     * <ul>
     *     <li>{@link ResponseMessages#UNAUTHORIZED}: Wrong login credentials</li>
     *     <li>{@link ResponseMessages#DATABASE_ERROR}: Error with the database</li>
     * </ul>
     */
    @POST
    @Path("/new-times")
    public Response newTimes(
            @Context SecurityContext securityContext,
            @NotNull List<TimeObject> timeObjects
    ) {
        if (securityContext.getUserPrincipal() == null) {
            return ResponseMessages.UNAUTHORIZED.getResponseBuilder().build();
        }

        try (MongoClient mongoclient = MongoClients.create("mongodb://localhost:27017")) {
            for (TimeObject timeObject : timeObjects) {
                mongoclient.getDatabase("worktime_server").getCollection("time_frame").insertOne(
                        new Document("employee_mail", securityContext.getUserPrincipal().getName())
                                .append("start", timeObject.getStart().toLocalDateTime())
                                .append("end", timeObject.getEnd().toLocalDateTime())
                                .append("status", "open"));
            }
        } catch (MongoException e) {
            return ResponseMessages.DATABASE_ERROR.getResponseBuilder().build();
        }

        return Response.ok().build();
    }

    /**
     * Get time frame of specified user.
     *
     * @param mongoClient database client.
     * @param username username to get time frame for
     * @return time frame entry or {@code null} if not found
     */
    private Document getUserTimestampEntry(MongoClient mongoClient, String username) {
        return mongoClient.getDatabase("worktime_server").getCollection("time_frame").find(
                Filters.and(
                        Filters.exists("end", false),
                        Filters.eq("employee_mail", username)
                )
        ).first();
    }

    /**
     * Check if an employee is already checked in.
     *
     * @param mongoClient database client.
     * @param username username to check
     * @return {@code true} if the employee is already checked in, {@code false} otherwise.
     */
    private boolean userAlreadyCheckedIn(MongoClient mongoClient, String username) {
        return getUserTimestampEntry(mongoClient, username) != null;
    }
}
