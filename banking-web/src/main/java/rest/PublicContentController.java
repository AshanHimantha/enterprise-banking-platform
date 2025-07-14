package rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.io.File;

@Path("/biller/logo")
public class PublicContentController {

    // Helper method to determine the correct directory path
    private String getBillerLogoDirectory() {
        // IMPORTANT: This must match the path used in AdminServiceImpl.java
        String webappPath = System.getProperty("com.sun.aas.instanceRoot");
        if (webappPath != null) {
            // Dynamic path on Payara server
            return webappPath + "/applications/banking-ear/assets/biller-logos/";
        } else {
            // Fallback path for local development
            return "C:\\banking_uploads\\biller-logos\\";
        }
    }

    @GET
    @Path("/image/{filename}")
    // Define the types of images we serve. JAX-RS will set the Content-Type header automatically.
    @Produces({"image/jpeg", "image/png", "image/gif"})
    public Response getBillerLogo(@PathParam("filename") String filename) {

        // Security Check: Prevent directory traversal attacks (e.g., "../../secrets.txt")
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid filename.").build();
        }

        // Construct the full path to the file on the server's disk
        File file = new File(getBillerLogoDirectory() + filename);

        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Logo not found.").build();
        }

        // Return the file stream. JAX-RS handles the rest.
        return Response.ok(file)
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .build();
    }
}