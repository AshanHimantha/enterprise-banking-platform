package rest;


import dto.BillerDTO;
import enums.BillerCategory;
import enums.BillerStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import service.BilllerService;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;


@Path("/admin/billers")
@RolesAllowed("ADMIN")
public class AdminBillerController {

    @EJB
    private BilllerService billlerService;


    public static class BillerStatusUpdateDTO {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Endpoint to create a new Biller with an optional logo.
     * Consumes multipart/form-data to handle both text fields and file uploads.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewBillerWithLogo(
            @FormDataParam("billerName") String billerName,
            @FormDataParam("category") String categoryStr,
            @FormDataParam("logo") InputStream logoStream,
            @FormDataParam("logo") FormDataContentDisposition logoDetails) {

        // --- Input Validation ---
        if (billerName == null || billerName.trim().isEmpty() || categoryStr == null || categoryStr.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "billerName and category are required fields."))
                    .build();
        }

        BillerCategory category;
        try {
            // Convert the category string from the form into its corresponding enum value.
            category = BillerCategory.valueOf(categoryStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Invalid category provided. Please use a valid category."))
                    .build();
        }

        String fileName = (logoDetails != null) ? logoDetails.getFileName() : null;

        // --- Service Call ---
        try {
            BillerDTO newBiller = billlerService.createBiller(billerName, category, logoStream, fileName);
            // On success, return 201 Created status and the new Biller object.
            return Response.status(Response.Status.CREATED).entity(newBiller).build();
        } catch (Exception e) {
            // Catch exceptions from the service layer (e.g., "Biller already exists").
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Endpoint to retrieve a list of all Billers in the system.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllBillers() {
        List<BillerDTO> billers = billlerService.getAllBillers();
        return Response.ok(billers).build();
    }

    /**
     * Endpoint to update the status of a specific Biller (e.g., to INACTIVE).
     */
    @POST
    @Path("/{billerId}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStatus(
            @PathParam("billerId") Long billerId,
            BillerStatusUpdateDTO statusUpdateDTO) {

        // --- Input Validation ---
        if (statusUpdateDTO == null || statusUpdateDTO.getStatus() == null || statusUpdateDTO.getStatus().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "The 'status' field is required in the request body."))
                    .build();
        }

        BillerStatus newStatus;
        try {
            newStatus = BillerStatus.valueOf(statusUpdateDTO.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Invalid status value provided."))
                    .build();
        }


        try {
            billlerService.updateBillerStatus(billerId, newStatus);
            return Response.ok(Collections.singletonMap("message", "Biller status updated successfully.")).build();
        } catch (Exception e) {
            // Catches errors like "Biller not found" from the service layer.
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }
}