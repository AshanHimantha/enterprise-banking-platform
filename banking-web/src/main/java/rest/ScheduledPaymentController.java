package rest;

import dto.ScheduleRequestDTO;
import dto.ScheduledPaymentDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import scheduler.ScheduledPaymentService;

import java.util.Collections;
import java.util.List;


@Path("/payments/schedule")
@RolesAllowed("CUSTOMER")
public class ScheduledPaymentController {

    @EJB
    private ScheduledPaymentService scheduledPaymentService;


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createScheduledPayment(ScheduleRequestDTO scheduleRequest, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            ScheduledPaymentDTO newSchedule = scheduledPaymentService.scheduleNewPayment(username, scheduleRequest);
            return Response.status(Response.Status.CREATED).entity(newSchedule).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Failed to create scheduled payment: " + e.getMessage()))
                    .build();
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyScheduledPayments(@Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            List<ScheduledPaymentDTO> schedules = scheduledPaymentService.getScheduledPaymentsForUser(username);
            return Response.ok(schedules).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("error", "Could not retrieve scheduled payments."))
                    .build();
        }
    }


    @POST
    @Path("/{scheduleId}/pause")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pauseSchedule(@PathParam("scheduleId") Long scheduleId, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            scheduledPaymentService.pauseScheduledPayment(username, scheduleId);
            return Response.ok(Collections.singletonMap("message", "Scheduled payment paused successfully.")).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(Collections.singletonMap("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }


    @POST
    @Path("/{scheduleId}/resume")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumeSchedule(@PathParam("scheduleId") Long scheduleId, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            scheduledPaymentService.resumeScheduledPayment(username, scheduleId);
            return Response.ok(Collections.singletonMap("message", "Scheduled payment resumed successfully.")).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(Collections.singletonMap("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }


    @DELETE
    @Path("/{scheduleId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelSchedule(@PathParam("scheduleId") Long scheduleId, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            scheduledPaymentService.cancelScheduledPayment(username, scheduleId);
            return Response.ok(Collections.singletonMap("message", "Scheduled payment canceled successfully.")).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(Collections.singletonMap("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }
}