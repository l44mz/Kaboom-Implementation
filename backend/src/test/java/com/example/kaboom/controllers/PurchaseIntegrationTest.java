package com.example.kaboom.controllers;

import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.ProductRepository;
import com.example.kaboom.repositories.PurchaseRepository;
import com.example.kaboom.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";
    private static final String PURCHASES_URL = "/api/v1/purchases";
    private static final String USERS_URL = "/api/v1/users";
    private static final String PASSWORD = "SecurePass123!";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PurchaseRepository purchaseRepository;


    // 1. Successful purchase

    @Test
    void shouldBuyProductAndDeductCoins() {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 100);
        Product product = createProduct(30, true);

        assertStatus(HttpStatus.CREATED, buy(token, product.getId()));
        assertEquals(70, coinsOf(email));

        ResponseEntity<String> items =
                send(HttpMethod.GET, USERS_URL + "/me/items", null, token);
        assertNotNull(items.getBody());
        assertTrue(items.getBody().contains(product.getName()));
    }


    // 2. Not enough coins

    @Test
    void shouldRejectPurchaseWithInsufficientCoins() {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 20);
        Product product = createProduct(30, true);

        assertStatus(HttpStatus.BAD_REQUEST, buy(token, product.getId()));
        assertEquals(20, coinsOf(email));

        ResponseEntity<String> items =
                send(HttpMethod.GET, USERS_URL + "/me/items", null, token);
        assertEquals("[]", items.getBody().trim());
    }


    // 3. Exactly enough coins

    @Test
    void shouldAllowPurchaseWithExactBalance() {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 30);
        Product product = createProduct(30, true);

        assertStatus(HttpStatus.CREATED, buy(token, product.getId()));
        assertEquals(0, coinsOf(email));
    }


    // 4. Already owned

    @Test
    void shouldRejectBuyingSameProductTwice() {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 100);
        Product product = createProduct(30, true);

        assertStatus(HttpStatus.CREATED, buy(token, product.getId()));
        assertStatus(HttpStatus.CONFLICT, buy(token, product.getId()));
        assertEquals(70, coinsOf(email));
    }


    // 5. Product not available

    @Test
    void shouldRejectUnavailableProduct() {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 100);
        Product product = createProduct(30, false);

        assertStatus(HttpStatus.CONFLICT, buy(token, product.getId()));
        assertEquals(100, coinsOf(email));
    }


    // 6. Nonexistent product

    @Test
    void shouldReturn404ForUnknownProduct() {

        String token = login(registerUser());

        assertStatus(HttpStatus.NOT_FOUND, buy(token, UUID.randomUUID()));
    }


    // 7. Missing productId

    @Test
    void shouldRejectPurchaseWithMissingProductId() {

        String token = login(registerUser());

        assertStatus(HttpStatus.BAD_REQUEST,
                send(HttpMethod.POST, PURCHASES_URL, "{}", token));
    }


    // 8. Authentication required

    @Test
    void shouldRequireAuthenticationToBuy() {

        Product product = createProduct(30, true);

        assertStatus(HttpStatus.UNAUTHORIZED, buy(null, product.getId()));
    }


    // 9. Concurrency: different products cannot overspend

    @Test
    void shouldNotOverspendWithConcurrentPurchases() throws Exception {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 100);
        Product p1 = createProduct(60, true);
        Product p2 = createProduct(60, true);

        int[] statuses = runConcurrently(
                () -> buy(token, p1.getId()),
                () -> buy(token, p2.getId())
        );

        assertEquals(1, count(statuses, 201), "statuses: " + Arrays.toString(statuses));
        assertEquals(1, count(statuses, 400), "statuses: " + Arrays.toString(statuses));
        assertEquals(40, coinsOf(email));
    }


    // 10. Concurrency: same product cannot be bought twice

    @Test
    void shouldNotDoubleChargeWithConcurrentSamePurchase() throws Exception {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 100);
        Product product = createProduct(30, true);

        int[] statuses = runConcurrently(
                () -> buy(token, product.getId()),
                () -> buy(token, product.getId())
        );

        assertEquals(1, count(statuses, 201), "statuses: " + Arrays.toString(statuses));
        assertEquals(1, count(statuses, 409), "statuses: " + Arrays.toString(statuses));
        assertEquals(70, coinsOf(email));
    }


    // 11. Equipping replaces another item in the same category

    @Test
    void shouldUnequipOtherItemInSameCategory() {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 100);
        Product p1 = createProduct(10, true);
        Product p2 = createProduct(10, true);
        buy(token, p1.getId());
        buy(token, p2.getId());

        assertStatus(HttpStatus.OK, equip(token, p1.getId(), true));
        assertTrue(isEquipped(email, p1));

        assertStatus(HttpStatus.OK, equip(token, p2.getId(), true));
        assertFalse(isEquipped(email, p1));
        assertTrue(isEquipped(email, p2));
    }


    // 12. Unequip is idempotent

    @Test
    void shouldUnequipItemIdempotently() {

        String email = registerUser();
        String token = login(email);
        setCoins(email, 100);
        Product product = createProduct(10, true);
        buy(token, product.getId());
        equip(token, product.getId(), true);

        assertStatus(HttpStatus.OK, equip(token, product.getId(), false));
        assertStatus(HttpStatus.OK, equip(token, product.getId(), false));
        assertFalse(isEquipped(email, product));
    }


    // 13. Cannot equip another user's item

    @Test
    void shouldNotAllowEquippingAnotherUsersItem() {

        String emailA = registerUser();
        String emailB = registerUser();
        String tokenA = login(emailA);
        String tokenB = login(emailB);
        setCoins(emailA, 100);
        Product product = createProduct(10, true);
        buy(tokenA, product.getId());

        assertStatus(HttpStatus.NOT_FOUND, equip(tokenB, product.getId(), true));
        assertFalse(isEquipped(emailA, product));
    }


    // 14. History only contains the caller's purchases

    @Test
    void shouldListOnlyOwnPurchases() {

        String emailA = registerUser();
        String emailB = registerUser();
        String tokenA = login(emailA);
        String tokenB = login(emailB);
        setCoins(emailA, 100);
        setCoins(emailB, 100);
        Product pA = createProduct(10, true);
        Product pB = createProduct(10, true);
        buy(tokenA, pA.getId());
        buy(tokenB, pB.getId());

        ResponseEntity<String> response = send(HttpMethod.GET, PURCHASES_URL, null, tokenA);

        assertStatus(HttpStatus.OK, response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains(pA.getName()));
        assertFalse(response.getBody().contains(pB.getName()));
    }


    // 15. Pagination bounds

    @Test
    void shouldRejectInvalidPaginationParameters() {

        String token = login(registerUser());

        assertStatus(HttpStatus.BAD_REQUEST,
                send(HttpMethod.GET, PURCHASES_URL + "?page=-1", null, token));
        assertStatus(HttpStatus.BAD_REQUEST,
                send(HttpMethod.GET, PURCHASES_URL + "?size=0", null, token));
        assertStatus(HttpStatus.BAD_REQUEST,
                send(HttpMethod.GET, PURCHASES_URL + "?size=101", null, token));
    }


    // 16. Database-level unique constraint

    @Test
    void shouldEnforceUniqueUserProductConstraint() {

        String email = registerUser();
        User user = userRepository.findByEmail(email).orElseThrow();
        Product product = createProduct(10, true);

        purchaseRepository.saveAndFlush(new Purchase(user, product, 10));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> purchaseRepository.saveAndFlush(new Purchase(user, product, 10))
        );
    }


    // Helpers

    private void assertStatus(HttpStatus expected, ResponseEntity<String> response) {
        assertEquals(expected, response.getStatusCode(), response.getBody());
    }

    private Product createProduct(int price, boolean available) {
        // Adjust this one method to match your Product entity.
        Product product = new Product();
        product.setName("Item-" + UUID.randomUUID());
        product.setCategory(Product.Category.values()[0]);
        product.setPrice(price);
        product.setAvailable(available);
        return productRepository.save(product);
    }

    private void setCoins(String email, int coins) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setCoins(coins);
        userRepository.save(user);
    }

    private int coinsOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getCoins();
    }

    private boolean isEquipped(String email, Product product) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return purchaseRepository
                .findByUser_IdAndProduct_Id(user.getId(), product.getId())
                .orElseThrow()
                .isEquipped();
    }

    private ResponseEntity<String> buy(String token, UUID productId) {
        String body = """
                {
                    "productId": "%s"
                }
                """.formatted(productId);
        return send(HttpMethod.POST, PURCHASES_URL, body, token);
    }

    private ResponseEntity<String> equip(String token, UUID productId, boolean equipped) {
        return send(HttpMethod.PATCH,
                USERS_URL + "/me/items/" + productId + "?equipped=" + equipped,
                null, token);
    }

    @SafeVarargs
    private int[] runConcurrently(Callable<ResponseEntity<String>>... calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            @SuppressWarnings("unchecked")
            Future<ResponseEntity<String>>[] futures = new Future[calls.length];
            for (int i = 0; i < calls.length; i++) {
                Callable<ResponseEntity<String>> call = calls[i];
                futures[i] = pool.submit(() -> {
                    start.await();
                    return call.call();
                });
            }
            start.countDown();

            int[] statuses = new int[calls.length];
            for (int i = 0; i < calls.length; i++) {
                statuses[i] = futures[i].get(30, TimeUnit.SECONDS).getStatusCode().value();
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private long count(int[] statuses, int status) {
        return Arrays.stream(statuses).filter(s -> s == status).count();
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

    private String login(String email) {
        String body = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        ResponseEntity<String> response =
                send(HttpMethod.POST, AUTH_URL + "/login", body, null);

        assertStatus(HttpStatus.OK, response);
        assertNotNull(response.getBody());
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