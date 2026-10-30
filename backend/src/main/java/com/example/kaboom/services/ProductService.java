
package com.example.kaboom.services;

import com.example.kaboom.dtos.ProductDto;
import com.example.kaboom.entities.Product;
import com.example.kaboom.repositories.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Returns only available products for regular shop users.
    public List<Product> getAllProducts() {
        return productRepository.findByIsAvailableTrue();
    }

    // Returns all products, including hidden ones, for admin management.
    public List<Product> getAllProductsForAdmin() {
        return productRepository.findAll();
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }

    public Product createProduct(ProductDto productDto) {
        Product product = new Product(
                productDto.getName(),
                productDto.getCategory(),
                productDto.getPrice(),
                productDto.getDescription()
        );

        product.setStock(productDto.getStock());

        return productRepository.save(product);
    }

    // Updates an existing product's details.
    public Product updateProduct(UUID id, ProductDto productDto) {
        Product product = getProductById(id);

        product.setName(productDto.getName());
        product.setCategory(productDto.getCategory());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        product.setStock(productDto.getStock());

        return productRepository.save(product);
    }

    // Shows or hides a product without deleting it.
    public Product setProductAvailability(UUID id, boolean available) {
        Product product = getProductById(id);

        product.setAvailable(available);

        return productRepository.save(product);
    }

    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
