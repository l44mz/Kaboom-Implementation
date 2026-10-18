package com.example.kaboom.services;

import com.example.kaboom.dtos.ProductDto;
import com.example.kaboom.entities.Product;
import com.example.kaboom.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public boolean addProduct(ProductDto productDto) {
        if (productDto == null) {
            return false;
        }
        Product product = new Product(
            productDto.getName(),
            productDto.getCategory(),
            productDto.getPrice(),
            productDto.getDescription()
        );
        Product saved = productRepository.save(product);
        return saved != null;
    }
}
