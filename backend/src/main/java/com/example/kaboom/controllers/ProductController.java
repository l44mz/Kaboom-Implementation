
package com.example.kaboom.controllers;

import com.example.kaboom.dtos.ProductDto;
import com.example.kaboom.entities.Product;
import com.example.kaboom.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Handles HTTP requests for retrieving and managing shop products.
 *
 * Product business logic is delegated to ProductService.
 * Creating products is restricted to authenticated administrators.
 */
@RestController
@RequestMapping("/products")
@Tag(
        name = "Products",
        description = "Retrieve available shop products and manage product creation."
)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Retrieves all products available for purchase in the shop.
     */
    @Operation(
            summary = "Get all available products",
            description = "Returns products currently available in the Kaboom shop."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Products retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = Product.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Retrieves a product by its unique identifier.
     */
    @Operation(
            summary = "Get a product by ID",
            description = "Returns the details of a specific product."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = Product.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Creates a new shop product.
     * Only users with the ADMIN role are authorized to perform this operation.
     */
    @Operation(
            summary = "Create a product (Admin only)",
            description = "Adds a new product to the shop. Requires an administrator account. "
                    + "The request includes the product name, category, price, and an optional description."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully",
                    content = @Content(
                            schema = @Schema(implementation = Product.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Only administrators can create products"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody ProductDto request) {

        Product createdProduct = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
}
