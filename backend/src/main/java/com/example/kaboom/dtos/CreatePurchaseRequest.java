
package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Contains the product identifier required to make a purchase.
 * The authenticated user's identity is obtained by the backend.
 */
@Schema(description = "Request body for purchasing a shop product.")
public class CreatePurchaseRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the product to purchase", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID productId;

    public CreatePurchaseRequest() {
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }
}
