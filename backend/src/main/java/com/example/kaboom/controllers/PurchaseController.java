
package com.example.kaboom.controllers;

import com.example.kaboom.dtos.CreatePurchaseRequest;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.services.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Handles product purchase requests and purchase history retrieval.
 *
 * Purchase business logic is delegated to PurchaseService.
 * The authenticated user's identity must come from the security context,
 * and users must only be able to access their own purchases.
 */
@RestController
@RequestMapping("/purchases")
@Tag(
        name = "Purchases",
        description = "Purchase shop products and retrieve purchase history."
)
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * Purchases a product using the authenticated user's coin balance.
     * The service must validate availability, ownership, and sufficient coins.
     */
    @Operation(
            summary = "Purchase a product",
            description = "Buys a product using the user's coins. "
                    + "The purchase and coin deduction must be handled atomically. "
                    + "The response includes the purchase and remaining coin balance."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Product purchased successfully",
                    content = @Content(
                            schema = @Schema(implementation = Purchase.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid purchase request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "Product not found or unavailable"),
            @ApiResponse(responseCode = "409", description = "Insufficient coins or product already owned"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping
    public ResponseEntity<Purchase> buyProduct(
            @Valid @RequestBody CreatePurchaseRequest request) {

        // TODO: Obtain the authenticated user's ID from the security context.
        // Do not accept a userId supplied by the client as proof of identity.
        throw new UnsupportedOperationException(
                "Connect the authenticated user to PurchaseService before enabling this endpoint."
        );
    }

    /**
     * Retrieves the authenticated user's purchase history.
     * The README specifies pagination using page and size query parameters.
     */
    @Operation(
            summary = "Get purchase history",
            description = "Returns the authenticated user's purchases. "
                    + "Supports pagination through the page and size query parameters."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase history retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = Purchase.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping
    public ResponseEntity<List<Purchase>> getUserPurchases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // TODO: Obtain the authenticated user's ID from the security context
        // and pass page and size to a paginated service method.
        throw new UnsupportedOperationException(
                "Implement authenticated, paginated purchase history in PurchaseService."
        );
    }
}
