package rest;


import auth.service.UserManagementService;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import dto.EmployeeCreateDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;

import java.util.Collections;


@Path("/admin/employees")
@RolesAllowed("ADMIN")
public class EmployeeManagementController {

    @EJB
    private UserManagementService userManagementService;

    public static class RoleRequestDTO {
        public String role;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEmployee(EmployeeCreateDTO createDTO) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(userManagementService.createEmployee(createDTO))
                    .build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            // *** THIS IS THE FIX ***
            // Catch specific, expected business rule exceptions and return their message.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            // Catch all other unexpected errors
            e.printStackTrace(); // Log the full error for debugging
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("error", "An unexpected server error occurred."))
                    .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmployees() {
        return Response.ok(userManagementService.getAllEmployees()).build();
    }

    @POST
    @Path("/{username}/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRole(@PathParam("username") String username, RoleRequestDTO roleDTO) {
        try {
            if (roleDTO == null || roleDTO.role == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Role is required.").build();
            }
            return Response.ok(userManagementService.addRoleToUser(username, roleDTO.role)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    @DELETE
    @Path("/{username}/roles/{roleName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeRole(@PathParam("username") String username, @PathParam("roleName") String roleName) {
        try {
            return Response.ok(userManagementService.removeRoleFromUser(username, roleName)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

}