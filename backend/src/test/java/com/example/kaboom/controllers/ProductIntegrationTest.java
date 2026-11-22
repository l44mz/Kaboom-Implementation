package com.example.kaboom.controllers;

import com.example.kaboom.dtos.ProductDto;
import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.ProductImageRepository;
import com.example.kaboom.repositories.ProductRepository;
import com.example.kaboom.repositories.PurchaseRepository;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.ProductImageService;
import com.example.kaboom.services.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProductIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";
    private static final String USERS_URL = "/api/v1/users";
    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String ADMIN_PRODUCTS_URL = "/api/v1/admin/products";
    private static final String PASSWORD = "SecurePass123!";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductImageRepository productImageRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private ProductService productService;
    @Autowired private ProductImageService productImageService;
    @Autowired private PasswordEncoder passwordEncoder;


    // ---------- Reading products ----------

    @Test
    void shouldRequireAuthenticationToListProducts() {

        assertStatus(HttpStatus.UNAUTHORIZED, send(HttpMethod.GET, PRODUCTS_URL, null, null));
    }

    @Test
    void shouldListOnlyAvailableProducts() {

        Product visible = saveProduct(true);
        Product hidden = saveProduct(false);
        String token = login(registerUser());

        ResponseEntity<String> response = send(HttpMethod.GET, PRODUCTS_URL, null, token);

        assertStatus(HttpStatus.OK, response);
        assertTrue(response.getBody().contains(visible.getName()));
        assertFalse(response.getBody().contains(hidden.getName()),
                "hidden product was listed to a normal user");
    }

    @Test
    void shouldGetProductById() {

        Product product = saveProduct(true);
        String token = login(registerUser());

        ResponseEntity<String> response =
                send(HttpMethod.GET, PRODUCTS_URL + "/" + product.getId(), null, token);

        assertStatus(HttpStatus.OK, response);
        assertTrue(response.getBody().contains(product.getName()));
    }

    @Test
    void shouldReturn404ForUnknownProduct() {

        String token = login(registerUser());

        assertStatus(HttpStatus.NOT_FOUND,
                send(HttpMethod.GET, PRODUCTS_URL + "/" + UUID.randomUUID(), null, token));
    }

    @Test
    void shouldRequireAuthenticationToGetProductById() {

        Product product = saveProduct(true);

        assertStatus(HttpStatus.UNAUTHORIZED,
                send(HttpMethod.GET, PRODUCTS_URL + "/" + product.getId(), null, null));
    }

    // Design decision: the list hides unavailable products, so fetching one by id should too.
    // Delete this test if you intentionally allow it.
    @Test
    void shouldNotExposeHiddenProductByIdToNormalUsers() {

        Product hidden = saveProduct(false);
        String token = login(registerUser());

        assertStatus(HttpStatus.NOT_FOUND,
                send(HttpMethod.GET, PRODUCTS_URL + "/" + hidden.getId(), null, token));
    }


    // ---------- Creating products: who is allowed ----------

    @Test
    void shouldRequireAuthenticationToCreateProduct() {

        assertStatus(HttpStatus.UNAUTHORIZED,
                send(HttpMethod.POST, PRODUCTS_URL, validProductJson(uniqueName()), null));
    }

    @Test
    void shouldRejectProductCreationByNormalUser() {

        String token = login(registerUser());
        long before = productRepository.count();

        assertStatus(HttpStatus.FORBIDDEN,
                send(HttpMethod.POST, PRODUCTS_URL, validProductJson(uniqueName()), token));
        assertEquals(before, productRepository.count());
    }

    @Test
    void shouldCreateProductAsAdmin() {

        String token = registerAdminAndLogin();
        String name = uniqueName();

        ResponseEntity<String> response =
                send(HttpMethod.POST, PRODUCTS_URL, validProductJson(name), token);

        assertStatus(HttpStatus.CREATED, response);

        UUID id = UUID.fromString(extractJsonValue(response.getBody(), "id"));
        Product saved = productRepository.findById(id).orElseThrow();

        assertEquals(name, saved.getName());
        assertEquals(Product.Category.hat, saved.getCategory());
        assertEquals(50, saved.getPrice());
        assertEquals(10, saved.getStock());
        assertTrue(saved.isAvailable());
    }

    @Test
    void shouldNotLetUserRegisterThemselvesAsAdmin() {

        String email = "test-" + UUID.randomUUID() + "@example.com";
        String body = """
                {
                    "name": "Sneaky User",
                    "email": "%s",
                    "password": "%s",
                    "role": "admin"
                }
                """.formatted(email, PASSWORD);

        ResponseEntity<String> registration =
                send(HttpMethod.POST, AUTH_URL + "/register", body, null);

        if (registration.getStatusCode() == HttpStatus.CREATED) {
            // extra field was ignored: the account must still be a normal user
            String token = login(email);
            assertStatus(HttpStatus.FORBIDDEN,
                    send(HttpMethod.POST, PRODUCTS_URL, validProductJson(uniqueName()), token));
        } else {
            // rejecting the unknown field outright is also acceptable
            assertStatus(HttpStatus.BAD_REQUEST, registration);
        }
    }


    // ---------- Creating products: validation ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"\",\"category\":\"hat\",\"price\":10,\"stock\":1}",
            "{\"category\":\"hat\",\"price\":10,\"stock\":1}",
            "{\"name\":\"X\",\"price\":10,\"stock\":1}",
            "{\"name\":\"X\",\"category\":\"weapon\",\"price\":10,\"stock\":1}",
            "{\"name\":\"X\",\"category\":\"hat\",\"price\":0,\"stock\":1}",
            "{\"name\":\"X\",\"category\":\"hat\",\"price\":-5,\"stock\":1}",
            "{\"name\":\"X\",\"category\":\"hat\",\"stock\":1}",
            "{\"name\":\"X\",\"category\":\"hat\",\"price\":10,\"stock\":-1}"
    })
    void shouldRejectInvalidProductData(String invalidBody) {

        String token = registerAdminAndLogin();
        long before = productRepository.count();

        assertStatus(HttpStatus.BAD_REQUEST,
                send(HttpMethod.POST, PRODUCTS_URL, invalidBody, token));
        assertEquals(before, productRepository.count());
    }


    // ---------- Product images: uploading ----------

    @Test
    void shouldUploadAndDownloadImageAsAdmin() {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);
        byte[] image = fakeImage(2_000);

        assertStatus(HttpStatus.NO_CONTENT,
                uploadImage(token, product.getId(), image, "image/png"));

        ResponseEntity<byte[]> download = getImage(token, product.getId());

        assertStatus(HttpStatus.OK, download);
        assertArrayEquals(image, download.getBody());
        assertEquals(MediaType.IMAGE_PNG, download.getHeaders().getContentType());
    }

    @Test
    void shouldReplaceExistingImage() {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);
        byte[] first = fakeImage(500);
        byte[] second = fakeImage(800);

        assertStatus(HttpStatus.NO_CONTENT, uploadImage(token, product.getId(), first, "image/png"));
        assertStatus(HttpStatus.NO_CONTENT, uploadImage(token, product.getId(), second, "image/jpeg"));

        ResponseEntity<byte[]> download = getImage(token, product.getId());

        assertArrayEquals(second, download.getBody());
        assertEquals(MediaType.IMAGE_JPEG, download.getHeaders().getContentType());
        assertTrue(productImageRepository.findByProductId(product.getId()).isPresent());
    }

    @Test
    void shouldRejectImageUploadByNormalUser() {

        String token = login(registerUser());
        Product product = saveProduct(true);

        assertStatus(HttpStatus.FORBIDDEN,
                uploadImage(token, product.getId(), fakeImage(100), "image/png"));
        assertTrue(productImageRepository.findByProductId(product.getId()).isEmpty());
    }

    @Test
    void shouldRequireAuthenticationToUploadImage() {

        Product product = saveProduct(true);

        assertStatus(HttpStatus.UNAUTHORIZED,
                uploadImage(null, product.getId(), fakeImage(100), "image/png"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    void shouldAcceptSupportedImageTypes(String contentType) {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);

        assertStatus(HttpStatus.NO_CONTENT,
                uploadImage(token, product.getId(), fakeImage(300), contentType));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "image/gif", "image/svg+xml", "text/plain", "application/pdf", "application/octet-stream"
    })
    void shouldRejectUnsupportedImageTypes(String contentType) {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);

        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                uploadImage(token, product.getId(), fakeImage(300), contentType));
        assertTrue(productImageRepository.findByProductId(product.getId()).isEmpty());
    }

    @Test
    void shouldRejectEmptyImageFile() {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);

        assertStatus(HttpStatus.BAD_REQUEST,
                uploadImage(token, product.getId(), new byte[0], "image/png"));
    }

    @Test
    void shouldRejectUploadWithoutImagePart() {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);

        // the form field must be called "image"
        assertStatus(HttpStatus.BAD_REQUEST,
                uploadImage(token, product.getId(), "photo", fakeImage(100), "image/png"));
    }

    @Test
    void shouldReturn404WhenUploadingImageForUnknownProduct() {

        String token = registerAdminAndLogin();

        assertStatus(HttpStatus.NOT_FOUND,
                uploadImage(token, UUID.randomUUID(), fakeImage(100), "image/png"));
    }

    // Spring's default multipart limit is 1 MB, but the service promises 5 MB.
    // This fails until spring.servlet.multipart.max-file-size is raised.
    @Test
    void shouldAcceptImagesBetweenOneAndFiveMegabytes() {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);

        assertStatus(HttpStatus.NO_CONTENT,
                uploadImage(token, product.getId(), fakeImage(1_500_000), "image/png"));
    }

    @Test
    void shouldAcceptImageOfExactlyFiveMegabytes() {

        Product product = saveProduct(true);
        MockMultipartFile file = new MockMultipartFile(
                "image", "max.png", "image/png", new byte[5 * 1024 * 1024]);

        assertDoesNotThrow(() -> productImageService.uploadImage(product.getId(), file));
    }

    @Test
    void shouldRejectImagesOverFiveMegabytes() {

        Product product = saveProduct(true);
        MockMultipartFile tooBig = new MockMultipartFile(
                "image", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productImageService.uploadImage(product.getId(), tooBig));

        assertEquals(413, ex.getStatusCode().value());
        assertTrue(productImageRepository.findByProductId(product.getId()).isEmpty());
    }


    // ---------- Product images: downloading and deleting ----------

    @Test
    void shouldReturn404WhenProductHasNoImage() {

        String token = login(registerUser());
        Product product = saveProduct(true);

        assertStatus(HttpStatus.NOT_FOUND, getImage(token, product.getId()));
    }

    @Test
    void shouldReturn404ForImageOfUnknownProduct() {

        String token = login(registerUser());

        assertStatus(HttpStatus.NOT_FOUND, getImage(token, UUID.randomUUID()));
    }

    @Test
    void shouldRequireAuthenticationToDownloadImage() {

        Product product = saveProduct(true);

        assertStatus(HttpStatus.UNAUTHORIZED, getImage(null, product.getId()));
    }

    @Test
    void shouldDeleteImageAsAdmin() {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);
        uploadImage(token, product.getId(), fakeImage(200), "image/png");

        assertStatus(HttpStatus.NO_CONTENT, deleteImage(token, product.getId()));
        assertStatus(HttpStatus.NOT_FOUND, getImage(token, product.getId()));
    }

    @Test
    void shouldReturn404WhenDeletingImageThatDoesNotExist() {

        String token = registerAdminAndLogin();
        Product product = saveProduct(true);

        assertStatus(HttpStatus.NOT_FOUND, deleteImage(token, product.getId()));
    }

    @Test
    void shouldReturn404WhenDeletingImageOfUnknownProduct() {

        String token = registerAdminAndLogin();

        assertStatus(HttpStatus.NOT_FOUND, deleteImage(token, UUID.randomUUID()));
    }

    @Test
    void shouldRejectImageDeletionByNormalUser() {

        String adminToken = registerAdminAndLogin();
        String userToken = login(registerUser());
        Product product = saveProduct(true);
        uploadImage(adminToken, product.getId(), fakeImage(200), "image/png");

        assertStatus(HttpStatus.FORBIDDEN, deleteImage(userToken, product.getId()));
        assertTrue(productImageRepository.findByProductId(product.getId()).isPresent());
    }

    @Test
    void shouldRequireAuthenticationToDeleteImage() {

        Product product = saveProduct(true);

        assertStatus(HttpStatus.UNAUTHORIZED, deleteImage(null, product.getId()));
    }


    // ---------- Admin operations that have no HTTP endpoint yet (service level) ----------

    @Test
    void shouldUpdateProduct() {

        Product product = saveProduct(true);
        ProductDto changes = new ProductDto(
                "Renamed-" + UUID.randomUUID(), Product.Category.screen, 99, "new description", 5);

        productService.updateProduct(product.getId(), changes);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(changes.getName(), updated.getName());
        assertEquals(Product.Category.screen, updated.getCategory());
        assertEquals(99, updated.getPrice());
        assertEquals("new description", updated.getDescription());
        assertEquals(5, updated.getStock());
    }

    @Test
    void shouldReturn404WhenUpdatingUnknownProduct() {

        ProductDto changes = new ProductDto("Name", Product.Category.hat, 10, null, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.updateProduct(UUID.randomUUID(), changes));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void shouldHideAndShowProductInShopList() {

        Product product = saveProduct(true);
        String token = login(registerUser());

        productService.setProductAvailability(product.getId(), false);
        assertFalse(send(HttpMethod.GET, PRODUCTS_URL, null, token)
                .getBody().contains(product.getName()));

        productService.setProductAvailability(product.getId(), true);
        assertTrue(send(HttpMethod.GET, PRODUCTS_URL, null, token)
                .getBody().contains(product.getName()));
    }

    @Test
    void shouldDeleteProductTogetherWithItsImage() {

        Product product = saveProduct(true);
        productImageService.uploadImage(product.getId(),
                new MockMultipartFile("image", "a.png", "image/png", fakeImage(100)));

        productService.deleteProduct(product.getId());

        assertFalse(productRepository.existsById(product.getId()));
        assertTrue(productImageRepository.findByProductId(product.getId()).isEmpty());
    }

    @Test
    void shouldReturn404WhenDeletingUnknownProduct() {

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.deleteProduct(UUID.randomUUID()));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void shouldNotBreakOwnersInventoryWhenProductIsDeleted() {

        String email = registerUser();
        String token = login(email);
        User owner = userRepository.findByEmail(email).orElseThrow();
        Product product = saveProduct(true);
        purchaseRepository.saveAndFlush(new Purchase(owner, product, product.getPrice()));

        try {
            productService.deleteProduct(product.getId());
        } catch (ResponseStatusException refused) {
            // A clean refusal is acceptable. A raw DataIntegrityViolationException is the bug:
            // purchases has a foreign key to products, so the database blocks the delete.
            assertEquals(409, refused.getStatusCode().value());
        }

        ResponseEntity<String> items = send(HttpMethod.GET, USERS_URL + "/me/items", null, token);

        assertStatus(HttpStatus.OK, items);
        assertTrue(items.getBody().contains(product.getName()),
                "owner lost an item: " + items.getBody());
    }


    // ---------- Helpers ----------

    private void assertStatus(HttpStatus expected, ResponseEntity<?> response) {
        Object body = response.getBody();
        String message = body instanceof byte[] ? "(binary body)" : String.valueOf(body);
        assertEquals(expected, response.getStatusCode(), message);
    }

    private String uniqueName() {
        return "Item-" + UUID.randomUUID();
    }

    private String validProductJson(String name) {
        return """
                {
                    "name": "%s",
                    "category": "hat",
                    "price": 50,
                    "description": "A test hat",
                    "stock": 10
                }
                """.formatted(name);
    }

    private Product saveProduct(boolean available) {
        Product product = new Product(uniqueName(), Product.Category.hat, 10, "test product");
        product.setAvailable(available);
        return productRepository.save(product);
    }

    private byte[] fakeImage(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 251);
        }
        return data;
    }

    private ResponseEntity<String> uploadImage(
            String token, UUID productId, byte[] data, String contentType) {
        return uploadImage(token, productId, "image", data, contentType);
    }

    private ResponseEntity<String> uploadImage(
            String token, UUID productId, String fieldName, byte[] data, String contentType) {

        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));

        ByteArrayResource resource = new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return "test-image";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(fieldName, new HttpEntity<>(resource, partHeaders));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                ADMIN_PRODUCTS_URL + "/" + productId + "/image",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private ResponseEntity<byte[]> getImage(String token, UUID productId) {

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                PRODUCTS_URL + "/" + productId + "/image",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);
    }

    private ResponseEntity<String> deleteImage(String token, UUID productId) {
        return send(HttpMethod.DELETE, ADMIN_PRODUCTS_URL + "/" + productId + "/image", null, token);
    }

    private String registerUser() {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String body = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        assertStatus(HttpStatus.CREATED,
                send(HttpMethod.POST, AUTH_URL + "/register", body, null));
        return email;
    }

    /** Creates an admin the same way AdminSeeder does, then logs in so the token carries the role.*/
    private String registerAdminAndLogin() {
        String email = "admin-" + UUID.randomUUID() + "@example.com";

        User admin = new User("Test Admin", email, passwordEncoder.encode(PASSWORD), "admin");
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);

        return login(email);
    }

    private String login(String email) {
        String body = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        ResponseEntity<String> response = send(HttpMethod.POST, AUTH_URL + "/login", body, null);

        assertStatus(HttpStatus.OK, response);
        return extractJsonValue(response.getBody(), "accessToken");
    }

    private ResponseEntity<String> send(HttpMethod method, String url, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    private String extractJsonValue(String json, String key) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return node.get(key).asText();
        } catch (Exception e) {
            throw new RuntimeException("Could not extract " + key, e);
        }
    }
}
