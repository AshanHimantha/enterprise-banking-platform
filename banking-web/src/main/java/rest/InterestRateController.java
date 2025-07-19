package com.ashanhimantha.ee.rest;


import dto.InterestRateDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import service.InterestService;

import java.util.Collections;
import java.util.List;

@Path("/admin/rates") // Only top-level admins can change interest rates
public class InterestRateController {

    @EJB
    private InterestService adminService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRates() {
        List<InterestRateDTO> rates = adminService.getAllInterestRates();
        return Response.ok(rates).build();
    }

    @RolesAllowed("ADMIN")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setRate(InterestRateDTO rateDTO) {
        try {
            InterestRateDTO savedRate = adminService.saveOrUpdateInterestRate(rateDTO);
            return Response.ok(savedRate).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }
}