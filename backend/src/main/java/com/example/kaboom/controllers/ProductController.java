package com.example.kaboom.controllers;

import com.example.kaboom.entities.Product;
import com.example.kaboom.services.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() { return ResponseEntity.ok(productService.getAllProducts()); }
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) { return ResponseEntity.ok(productService.getProductById(id)); }
}