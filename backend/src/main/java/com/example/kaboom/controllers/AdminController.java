
package com.example.kaboom.controllers;

import com.example.kaboom.dtos.ProductDto;
import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.repositories.FocusSessionRepository;
import com.example.kaboom.repositories.ProductRepository;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@Tag(
        name = "Admin",
        description = "Administrative operations for managing the store and dashboard"
)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final RobotRepository robotRepository;
    private final ProductRepository productRepository;

    public AdminController(
            ProductService productService,
            UserRepository userRepository,
            FocusSessionRepository focusSessionRepository,
            RobotRepository robotRepository,
            ProductRepository productRepository
    ) {
        this.productService = productService;
        this.userRepository = userRepository;
        this.focusSessionRepository = focusSessionRepository;
        this.robotRepository = robotRepository;
        this.productRepository = productRepository;
    }

    // Return the statistics displayed on the admin dashboard.
    @GetMapping("/dashboard/stats")
    @Operation(
            summary = "Get admin dashboard statistics",
            description = "Returns regular user count, today's focus sessions, connected robots, and total store products."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<AdminDashboardStatsResponse> getDashboardStats() {

        // Use Saudi Arabia's local date to calculate today's sessions.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Riyadh"));
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        long totalUsers = userRepository.countByRole("user");

        long sessionsToday = focusSessionRepository.countSessionsStartedBetween(
                startOfDay,
                startOfTomorrow
        );

        long robotsOnline = robotRepository.countByConnectionState(
                Robot.ConnectionState.connected
        );

        // Count all products, including hidden products.
        long storeProducts = productRepository.count();

        return ResponseEntity.ok(
                new AdminDashboardStatsResponse(
                        totalUsers,
                        sessionsToday,
                        robotsOnline,
                        storeProducts
                )
        );
    }

    // Create a new store product.
    @PostMapping("/products")
    @Operation(
            summary = "Create a product",
            description = "Creates a new store product. Admin access is required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product information")
    })
    public ResponseEntity<Product> addProduct(
            @Valid @RequestBody ProductDto productDto) {

        Product createdProduct = productService.createProduct(productDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProduct);
    }

    // Retrieve all products, including hidden products.
    @GetMapping("/products")
    @Operation(
            summary = "Get all products for admin",
            description = "Retrieves all products, including hidden products."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProductsForAdmin());
    }

    // Retrieve a product by its ID.
    @GetMapping("/products/{id}")
    @Operation(
            summary = "Get a product by ID",
            description = "Retrieves a specific product using its unique identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // Update an existing product.
    @PutMapping("/products/{id}")
    @Operation(
            summary = "Update a product",
            description = "Updates an existing product's name, category, price, description, and stock."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product information"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Product> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductDto productDto) {

        Product updatedProduct = productService.updateProduct(id, productDto);

        return ResponseEntity.ok(updatedProduct);
    }

    // Show or hide a product without deleting it.
    @PatchMapping("/products/{id}/availability")
    @Operation(
            summary = "Show or hide a product",
            description = "Changes product availability without deleting it. Admin access is required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product availability updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Product> setProductAvailability(
            @PathVariable UUID id,
            @RequestParam boolean available) {

        Product updatedProduct =
                productService.setProductAvailability(id, available);

        return ResponseEntity.ok(updatedProduct);
    }

    // Delete a store product.
    @DeleteMapping("/products/{id}")
    @Operation(
            summary = "Delete a product",
            description = "Deletes a store product by its ID. Admin access is required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Response model for the admin dashboard statistics.
    public record AdminDashboardStatsResponse(
            long totalUsers,
            long sessionsToday,
            long robotsOnline,
            long storeProducts
    ) {
    }
}
