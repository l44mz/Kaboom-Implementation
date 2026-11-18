
package com.example.kaboom.services;

import com.example.kaboom.dtos.ProductDto;
import com.example.kaboom.entities.Product;
import com.example.kaboom.repositories.ProductImageRepository;
import com.example.kaboom.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private Product product;
    private ProductDto productDto;
    private Product.Category category;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = mock(Product.class);
        productDto = mock(ProductDto.class);
        category = Product.Category.values()[0];

        lenient().when(productDto.getName()).thenReturn("Wizard Hat");
        lenient().when(productDto.getCategory()).thenReturn(category);
        lenient().when(productDto.getPrice()).thenReturn(50);
        lenient().when(productDto.getDescription())
                .thenReturn("A magical hat");
        lenient().when(productDto.getStock()).thenReturn(10);
    }

    // -------------------------------------------------------
    // GET ALL PRODUCTS
    // -------------------------------------------------------

    @Test
    void getAllProducts_returnsAvailableProducts() {
        List<Product> products = List.of(product);

        when(productRepository.findByIsAvailableTrue())
                .thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(products, result);

        verify(productRepository).findByIsAvailableTrue();
        verify(productRepository, never()).findAll();
    }

    @Test
    void getAllProducts_noProducts_returnsEmptyList() {
        when(productRepository.findByIsAvailableTrue())
                .thenReturn(List.of());

        List<Product> result = productService.getAllProducts();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository).findByIsAvailableTrue();
    }

    // -------------------------------------------------------
    // GET ALL PRODUCTS FOR ADMIN
    // -------------------------------------------------------

    @Test
    void getAllProductsForAdmin_returnsAllProducts() {
        List<Product> products = List.of(product);

        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProductsForAdmin();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(products, result);

        verify(productRepository).findAll();
    }

    @Test
    void getAllProductsForAdmin_noProducts_returnsEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<Product> result = productService.getAllProductsForAdmin();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository).findAll();
    }

    // -------------------------------------------------------
    // GET PRODUCT BY ID
    // -------------------------------------------------------

    @Test
    void getProductById_productExists_returnsProduct() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        Product result = productService.getProductById(productId);

        assertNotNull(result);
        assertSame(product, result);

        verify(productRepository).findById(productId);
    }

    @Test
    void getProductById_productNotFound_throwsNotFound() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductById(productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productRepository).findById(productId);
    }

    // -------------------------------------------------------
    // CREATE PRODUCT
    // -------------------------------------------------------

    @Test
    void createProduct_validDto_createsAndSavesProduct() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(productDto);

        assertNotNull(result);

        ArgumentCaptor<Product> captor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(captor.capture());

        Product savedProduct = captor.getValue();

        assertEquals("Wizard Hat", savedProduct.getName());
        assertEquals(category, savedProduct.getCategory());
        assertEquals(50, savedProduct.getPrice());
        assertEquals("A magical hat", savedProduct.getDescription());
        assertEquals(10, savedProduct.getStock());

        assertSame(savedProduct, result);
    }

    // -------------------------------------------------------
    // UPDATE PRODUCT
    // -------------------------------------------------------

    @Test
    void updateProduct_productExists_updatesAndSavesProduct() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.updateProduct(
                productId, productDto
        );

        assertNotNull(result);
        assertSame(product, result);

        verify(product).setName("Wizard Hat");
        verify(product).setCategory(category);
        verify(product).setPrice(50);
        verify(product).setDescription("A magical hat");
        verify(product).setStock(10);

        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_productNotFound_throwsNotFound() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(productId, productDto)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productRepository, never()).save(any(Product.class));
    }

    // -------------------------------------------------------
    // SET PRODUCT AVAILABILITY
    // -------------------------------------------------------

    @Test
    void setProductAvailability_makeAvailable_updatesProduct() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.setProductAvailability(
                productId, true
        );

        assertNotNull(result);
        assertSame(product, result);

        verify(product).setAvailable(true);
        verify(productRepository).save(product);
    }

    @Test
    void setProductAvailability_makeUnavailable_updatesProduct() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.setProductAvailability(
                productId, false
        );

        assertNotNull(result);
        assertSame(product, result);

        verify(product).setAvailable(false);
        verify(productRepository).save(product);
    }

    @Test
    void setProductAvailability_productNotFound_throwsNotFound() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.setProductAvailability(
                        productId, true
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productRepository, never()).save(any(Product.class));
    }

    // -------------------------------------------------------
    // DELETE PRODUCT
    // -------------------------------------------------------

    @Test
    void deleteProduct_productExists_deletesImageThenProduct() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        InOrder inOrder = inOrder(
                productImageRepository,
                productRepository
        );

        inOrder.verify(productImageRepository)
                .deleteByProductId(productId);

        inOrder.verify(productRepository).delete(product);

        verify(productRepository).findById(productId);
    }

    @Test
    void deleteProduct_productNotFound_throwsNotFound() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.deleteProduct(productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productImageRepository, never())
                .deleteByProductId(any(UUID.class));

        verify(productRepository, never()).delete(any(Product.class));
    }
}

