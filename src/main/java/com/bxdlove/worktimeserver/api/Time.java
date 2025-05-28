package com.bxdlove.worktimeserver.api;

import com.bxdlove.worktimeserver.api.json.TimeObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.*;
import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.Document;

import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * This class handles employee time entries.
 * It provides the following endpoints:
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
            Document userTimeStampEntry = getCurrentUserTimestampEntry(mongoclient, securityContext.getUserPrincipal().getName());
            if (userTimeStampEntry == null) {
                return ResponseMessages.NO_ACTIVE_TIME_FRAME.getResponseBuilder().build();
            }

            mongoclient.getDatabase("worktime_server").getCollection("time_frame").updateOne(
                    new Document("employee_mail", securityContext.getUserPrincipal().getName()).append("_id", userTimeStampEntry.get("_id")),
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
                                .append("start", timeObject.getStart().toInstant())
                                .append("end", timeObject.getEnd().toInstant())
                                .append("status", "open"));
            }
        } catch (MongoException e) {
            return ResponseMessages.DATABASE_ERROR.getResponseBuilder().build();
        }

        return Response.ok().build();
    }

    /**
     * Receive all time frames of user.
     *
     * @param securityContext provides secruity-related context
     * @return a response indicating the result of the operation:
     * <ul>
     *     <li>{@link ResponseMessages#UNAUTHORIZED}: Wrong login credentials</li>
     *     <li>{@link ResponseMessages#DATABASE_ERROR}: Error with the database</li>
     *     <li>{@link Response#ok()}: Contains all times</li>
     * </ul>
     */
    @GET
    public Response getAllTimes(
            @Context SecurityContext securityContext
    ) {
        if (securityContext.getUserPrincipal() == null) {
            return ResponseMessages.UNAUTHORIZED.getResponseBuilder().build();
        }

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            mongoClient.getDatabase("worktime_server").getCollection("time_frame").find(
                    Filters.eq("employee_mail", securityContext.getUserPrincipal().getName())
            ).forEach(document -> arrayBuilder.add(Json.createObjectBuilder()
                    .add("start", document.get("start", Date.class).toInstant().toString())
                    .add("end", Objects.requireNonNullElse(document.get("end", Date.class), Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())).toInstant().toString()).build()
            ));
        } catch (MongoException e) {
            return ResponseMessages.DATABASE_ERROR.getResponseBuilder().build();
        }

        return Response.ok(arrayBuilder.build()).build();
    }

    @GET
    @Path("/status")
    public Response getStatus(
            @Context SecurityContext securityContext
    ) {
        if (securityContext.getUserPrincipal() == null) {
            return ResponseMessages.UNAUTHORIZED.getResponseBuilder().build();
        }

        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            Document userTimeStampEntry = getCurrentUserTimestampEntry(mongoClient, securityContext.getUserPrincipal().getName());
            if (userTimeStampEntry == null) {
                return Response.ok(TimeStatusCode.NOT_LOGGED_IN.getReturnCode()).build();
            }

            if (isHoliday()) {
                return Response.ok(TimeStatusCode.HOLIDAY.getReturnCode()).build();
            }

            Instant stampInTime = userTimeStampEntry.get("start", Date.class).toInstant();
            LocalDateTime localStampInDateTime = stampInTime.atZone(TimeZone.getDefault().toZoneId()).toLocalDateTime();
            Document lastStampEntry = getLastStampEntry(mongoClient, securityContext.getUserPrincipal().getName());

            if (lastStampEntry == null) {
                return Response.ok(TimeStatusCode.OK).build();
            }

            if (!isCoreWorkingHours(localStampInDateTime.toLocalTime())) {
                return Response.ok(TimeStatusCode.OUTSIDE_CORE_WORKING_HOURS.getReturnCode()).build();
            }

            if (isMinor(mongoClient, securityContext.getUserPrincipal().getName())) {
                return Response.ok(handleMinor(stampInTime).getReturnCode()).build();
            }

            if (breakNeeded(stampInTime)) {
                return Response.ok(TimeStatusCode.BREAK_NEEDED.getReturnCode()).build();
            }
        } catch (MongoException e) {
            return ResponseMessages.DATABASE_ERROR.getResponseBuilder().build();
        }

        return Response.ok(TimeStatusCode.OK).build();
    }

    private boolean isHoliday() {
        return LocalDateTime.now().getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private boolean isCoreWorkingHours(LocalTime localStampInTime) {
        return localStampInTime.isBefore(LocalTime.of(22,0)) && localStampInTime.isAfter(LocalTime.of(6, 0));
    }

    private boolean breakNeeded(Instant stampIn) {
        return Duration.between(stampIn, LocalDateTime.now()).toHours() >= 6;
    }

    private boolean isMinor(MongoClient mongoClient, String user) {
        boolean b = false;

        Document document = mongoClient.getDatabase("worktime_server").getCollection("employee").find(
                Filters.eq("email", user)
        ).first();

        if (document != null) {
            b = document.get("is_minor", Boolean.class);
        }

        return b;
    }

    private TimeStatusCode handleMinor(Instant stampInTime) {
        if (Duration.between(stampInTime, LocalDateTime.now()).toHours() > 6) {
            return TimeStatusCode.BREAK_NEEDED;
        }

        return TimeStatusCode.OK;
    }

    /**
     * Get time frame of specified user.
     *
     * @param mongoClient database client.
     * @param username username to get time frame for
     * @return time frame entry or {@code null} if not found
     */
    private Document getCurrentUserTimestampEntry(MongoClient mongoClient, String username) {
        return mongoClient.getDatabase("worktime_server").getCollection("time_frame").find(
                Filters.and(
                        Filters.exists("end", false),
                        Filters.eq("employee_mail", username)
                )
        ).first();
    }

    /**
     * @param mongoClient database client.
     * @return last stamp entry of specified user.
     */
    private Document getLastStampEntry(MongoClient mongoClient, String username) {
        return mongoClient.getDatabase("worktime_server")
                .getCollection("time_frame")
                .find(Filters.eq("email", username))
                .sort(Sorts.descending("end"))
                .limit(1)
                .first();
    }

    /**
     * Check if an employee is already checked in.
     *
     * @param mongoClient database client.
     * @param username username to check
     * @return {@code true} if the employee is already checked in, {@code false} otherwise.
     */
    private boolean userAlreadyCheckedIn(MongoClient mongoClient, String username) {
        return getCurrentUserTimestampEntry(mongoClient, username) != null;
    }
}
