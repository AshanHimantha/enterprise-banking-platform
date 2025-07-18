package rest;

import dto.*;
import service.VirtualCardService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Path("/cards/virtual")
@RolesAllowed("CUSTOMER")
public class VirtualCardController {

    @EJB
    private VirtualCardService virtualCardService;



    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCard(CreateVirtualCardDTO requestDTO, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            VirtualCardDTO newCard = virtualCardService.createVirtualCard(username, requestDTO);
            return Response.status(Response.Status.CREATED).entity(newCard).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }

    /**
     * Endpoint to retrieve all virtual cards for the logged-in user.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMyCards(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        List<VirtualCardDTO> cards = virtualCardService.getVirtualCardsForUser(username);
        return Response.ok(cards).build();
    }

    /**
     * Endpoint to temporarily freeze an active virtual card.
     */
    @POST
    @Path("/{cardId}/freeze")
    @Produces(MediaType.APPLICATION_JSON)
    public Response freezeCard(@PathParam("cardId") Long cardId, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            VirtualCardDTO updatedCard = virtualCardService.freezeVirtualCard(username, cardId);
            return Response.ok(updatedCard).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }

    /**
     * Endpoint to unfreeze a frozen virtual card.
     */
    @POST
    @Path("/{cardId}/unfreeze")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unfreezeCard(@PathParam("cardId") Long cardId, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            VirtualCardDTO updatedCard = virtualCardService.unfreezeVirtualCard(username, cardId);
            return Response.ok(updatedCard).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }

    /**
     * Endpoint to permanently terminate a virtual card. This is an irreversible action.
     */
    @DELETE
    @Path("/{cardId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response terminateCard(@PathParam("cardId") Long cardId, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            virtualCardService.terminateVirtualCard(username, cardId);
            return Response.ok(Collections.singletonMap("message", "Virtual card terminated successfully.")).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(Collections.singletonMap("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap("error", "An unexpected error occurred.")).build();
        }
    }

    /**
     * Endpoint to set or update the spending limit for a virtual card.
     */
    @PUT
    @Path("/{cardId}/limit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setLimit(@PathParam("cardId") Long cardId, SpendingLimitDTO limitDTO, @Context SecurityContext securityContext) {
        try {
            if (limitDTO == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "Request body is missing.")).build();
            }
            String username = securityContext.getUserPrincipal().getName();
            VirtualCardDTO updatedCard = virtualCardService.updateSpendingLimit(username, cardId, limitDTO.getNewLimit());
            return Response.ok(updatedCard).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }

    /**
     * Endpoint to set or change the 4-digit PIN for a card.
     * This action requires the user's main login password for authorization.
     */
    @POST
    @Path("/{cardId}/pin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setPin(@PathParam("cardId") Long cardId, PinChangeDTO pinDTO, @Context SecurityContext securityContext) {
        try {
            if (pinDTO == null || pinDTO.getCurrentPassword() == null || pinDTO.getNewPin() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "currentPassword and newPin are required.")).build();
            }
            String username = securityContext.getUserPrincipal().getName();
            virtualCardService.setOrChangePin(username, cardId, pinDTO.getCurrentPassword(), pinDTO.getNewPin());
            return Response.ok(Collections.singletonMap("message", "PIN updated successfully.")).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Collections.singletonMap("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", e.getMessage())).build();
        }
    }


    @POST
    @Path("/{cardId}/reveal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revealCardDetails(@PathParam("cardId") Long cardId,
                                      RevealRequestDTO revealRequest,
                                      @Context SecurityContext securityContext) {

        if (revealRequest == null || revealRequest.getCurrentPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "Your current password is required to reveal card details.")).build();
        }

        try {
            String username = securityContext.getUserPrincipal().getName();
            Optional<UnmaskedVirtualCardDTO> unmaskedCard = virtualCardService.getUnmaskedCardDetails(
                    username,
                    cardId,
                    revealRequest.getCurrentPassword()
            );

            // If the service returns the DTO, send it back with a 200 OK.
            // Note: The frontend should only display this information temporarily and never store it.
            return Response.ok(unmaskedCard.get()).build();

        } catch (SecurityException e) {
            // This catches an incorrect password.
            return Response.status(Response.Status.UNAUTHORIZED).entity(Collections.singletonMap("error", e.getMessage())).build();
        } catch (IllegalStateException e) {
            // This catches trying to reveal a terminated card.
            return Response.status(Response.Status.FORBIDDEN).entity(Collections.singletonMap("error", e.getMessage())).build();
        } catch (Exception e) {
            // This catches "Card not found" or other errors.
            return Response.status(Response.Status.NOT_FOUND).entity(Collections.singletonMap("error", "Card not found or an error occurred.")).build();
        }
    }

}