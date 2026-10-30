package com.example.kaboom.controllers;

import com.example.kaboom.entities.ProductImage;
import com.example.kaboom.services.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Handles HTTP requests for uploading, retrieving,
 * and deleting product images.
 */
@RestController
@RequestMapping
@Tag(
        name = "Product Images",
        description = "Operations for managing product images"
)
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    /**
     * Uploads or replaces an image for a specific product.
     * Only authenticated administrators can upload images.
     */
    @PostMapping(
            value = "/admin/products/{productId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Upload a product image",
            description = "Uploads or replaces a product image. Admin access is required."
    )
    public ResponseEntity<Void> uploadProductImage(
            @PathVariable UUID productId,
            @RequestParam("image") MultipartFile image
    ) {
        productImageService.uploadImage(productId, image);

        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the image associated with a specific product.
     * Authentication is required to access product images.
     */
    @GetMapping("/products/{productId}/image")
    @Operation(
            summary = "Get a product image",
            description = "Returns the actual image file associated with a product."
    )
    public ResponseEntity<byte[]> getProductImage(
            @PathVariable UUID productId
    ) {
        ProductImage productImage =
                productImageService.getImage(productId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        productImage.getContentType()
                ))
                .header(
                        HttpHeaders.CONTENT_LENGTH,
                        String.valueOf(productImage.getFileSize())
                )
                .body(productImage.getImageData());
    }

    /**
     * Deletes the image associated with a specific product.
     * Only authenticated administrators can delete images.
     */
    @DeleteMapping("/admin/products/{productId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a product image",
            description = "Deletes the stored image for a product. Admin access is required."
    )
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable UUID productId
    ) {
        productImageService.deleteImage(productId);

        return ResponseEntity.noContent().build();
    }
}
