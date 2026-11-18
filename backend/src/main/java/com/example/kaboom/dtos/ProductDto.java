
package com.example.kaboom.dtos;

import com.example.kaboom.entities.Product.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains the product information required to create a shop product.
 * The product ID and availability status are managed by the system.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request body containing the details of a shop product.")
public class ProductDto {

    @NotBlank
    @Schema(
            description = "Name of the product",
            example = "Wizard Hat",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @NotNull
    @Schema(
            description = "Product category: hat, screen, or effect",
            example = "hat",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Category category;

    @Positive
    @Schema(
            description = "Product price in coins. Must be greater than zero.",
            example = "50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int price;

    @Schema(
            description = "Optional product description",
            example = "A magical hat for Sparky"
    )
    private String description;

    @Min(0)
    @Schema(
            description = "Number of units available in stock. Set by the admin.",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int stock;

    public ProductDto(
            String name,
            Category category,
            int price,
            String description,
            int stock
    ) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.stock = stock;
    }
}
