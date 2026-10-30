package com.example.kaboom.services;

import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.ProductImage;
import com.example.kaboom.repositories.ProductImageRepository;
import com.example.kaboom.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Handles product image operations, including validation,
 * database storage, retrieval, and deletion.
 */
@Service
public class ProductImageService {

    // Maximum allowed image size: 5 MB.
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    // Supported image formats.
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public ProductImageService(
            ProductImageRepository productImageRepository,
            ProductRepository productRepository
    ) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
    }

    /**
     * Validates and stores an image for the specified product.
     * If an image already exists, its data is replaced.
     */
    @Transactional
    public void uploadImage(UUID productId, MultipartFile file) {

        // Ensure that an image file was provided.
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image file is required"
            );
        }

        // Reject images that exceed the maximum allowed size.
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image size must not exceed 5 MB"
            );
        }

        // Validate the declared image content type.
        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG and WebP images are allowed"
            );
        }

        // Verify that the target product exists.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        try {
            // Retrieve the existing image or create a new image record.
            ProductImage productImage = productImageRepository
                    .findByProductId(productId)
                    .orElseGet(ProductImage::new);

            // Associate the image with its product.
            productImage.setProduct(product);

            // Store the actual binary image data in the database.
            productImage.setImageData(file.getBytes());

            // Store image metadata for future retrieval.
            productImage.setContentType(contentType.toLowerCase());
            productImage.setFileSize(file.getSize());

            // Save the image record in MySQL.
            productImageRepository.save(productImage);

        } catch (IOException exception) {
            // Return an error if the uploaded file cannot be read.
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read image file"
            );
        }
    }

    /**
     * Retrieves the stored image associated with a product.
     */
    @Transactional
    public ProductImage getImage(UUID productId) {

        // Find the image using its associated product ID.
        return productImageRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product image not found"
                ));
    }

    /**
     * Deletes the image associated with the specified product.
     */
    @Transactional
    public void deleteImage(UUID productId) {

        // Ensure that the product exists before deleting its image.
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found"
            );
        }

        // Ensure that the product has an image to delete.
        if (productImageRepository.findByProductId(productId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product image not found"
            );
        }

        // Delete the image record from the database.
        productImageRepository.deleteByProductId(productId);
    }
}
