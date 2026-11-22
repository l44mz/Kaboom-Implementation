package com.example.kaboom.services;

import com.example.kaboom.dtos.OwnedItemDto;
import com.example.kaboom.dtos.PurchaseResponse;
import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.ProductRepository;
import com.example.kaboom.repositories.PurchaseRepository;
import com.example.kaboom.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    private User user;
    private Product product;

    private final String email = "test@example.com";
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        product = mock(Product.class);

        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getCoins()).thenReturn(100);

        lenient().when(product.getId()).thenReturn(productId);
        lenient().when(product.getName()).thenReturn("Wizard Hat");
        lenient().when(product.getCategory())
                .thenReturn(Product.Category.values()[0]);
        lenient().when(product.getPrice()).thenReturn(30);
        lenient().when(product.isAvailable()).thenReturn(true);
    }

    // -------------------------------------------------------
    // BUY PRODUCT
    // -------------------------------------------------------

    @Test
    void buyProduct_validPurchase_savesPurchaseAndDeductsCoins() {
        Purchase savedPurchase = mock(Purchase.class);

        when(userRepository.findByEmailForUpdate(email))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(purchaseRepository.existsByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(false);

        when(purchaseRepository.save(any(Purchase.class)))
                .thenReturn(savedPurchase);

        when(savedPurchase.getProduct()).thenReturn(product);
        when(savedPurchase.getId()).thenReturn(UUID.randomUUID());
        when(savedPurchase.getPricePaid()).thenReturn(30);
        when(savedPurchase.getPurchasedAt())
                .thenReturn(java.time.LocalDateTime.now());
        when(savedPurchase.isEquipped()).thenReturn(false);

        PurchaseResponse response =
                purchaseService.buyProduct(email, productId);

        assertNotNull(response);

        verify(user).setCoins(70);
        verify(userRepository).save(user);

        ArgumentCaptor<Purchase> captor =
                ArgumentCaptor.forClass(Purchase.class);

        verify(purchaseRepository).save(captor.capture());

        Purchase createdPurchase = captor.getValue();

        assertSame(user, createdPurchase.getUser());
        assertSame(product, createdPurchase.getProduct());
        assertEquals(1, createdPurchase.getQuantity());
        assertEquals(30, createdPurchase.getPricePaid());
        assertEquals(30, createdPurchase.getTotalPrice());
        assertNotNull(createdPurchase.getPurchasedAt());
    }

    @Test
    void buyProduct_userNotFound_throwsNotFound() {
        when(userRepository.findByEmailForUpdate(email))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.buyProduct(email, productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(productRepository);
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void buyProduct_productNotFound_throwsNotFound() {
        when(userRepository.findByEmailForUpdate(email))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.buyProduct(email, productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(purchaseRepository);
        verify(user, never()).setCoins(anyInt());
    }

    @Test
    void buyProduct_productNotAvailable_throwsConflict() {
        when(userRepository.findByEmailForUpdate(email))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(product.isAvailable()).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.buyProduct(email, productId)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(purchaseRepository, never())
                .existsByUser_IdAndProduct_Id(any(), any());

        verify(user, never()).setCoins(anyInt());
    }

    @Test
    void buyProduct_alreadyOwned_throwsConflict() {
        when(userRepository.findByEmailForUpdate(email))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(purchaseRepository.existsByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.buyProduct(email, productId)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(user, never()).setCoins(anyInt());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void buyProduct_notEnoughCoins_throwsBadRequest() {
        when(userRepository.findByEmailForUpdate(email))
                .thenReturn(Optional.of(user));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(purchaseRepository.existsByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(false);

        when(user.getCoins()).thenReturn(10);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.buyProduct(email, productId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(user, never()).setCoins(anyInt());
        verify(userRepository, never()).save(any(User.class));
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    // -------------------------------------------------------
    // GET USER PURCHASES
    // -------------------------------------------------------

    @Test
    void getUserPurchases_validUser_returnsPurchasePage() {
        Pageable pageable = PageRequest.of(0, 10);

        Purchase purchase = mock(Purchase.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findByUser_Id(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(purchase)));

        when(purchase.getProduct()).thenReturn(product);
        when(purchase.getId()).thenReturn(UUID.randomUUID());
        when(purchase.getPricePaid()).thenReturn(30);
        when(purchase.getPurchasedAt())
                .thenReturn(java.time.LocalDateTime.now());
        when(purchase.isEquipped()).thenReturn(false);

        Page<PurchaseResponse> result =
                purchaseService.getUserPurchases(email, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(userRepository).findByEmail(email);
        verify(purchaseRepository).findByUser_Id(userId, pageable);
    }

    @Test
    void getUserPurchases_userNotFound_throwsNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.getUserPurchases(email, pageable)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void getUserPurchases_noPurchases_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findByUser_Id(userId, pageable))
                .thenReturn(Page.empty());

        Page<PurchaseResponse> result =
                purchaseService.getUserPurchases(email, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------
    // GET USER ITEMS
    // -------------------------------------------------------

    @Test
    void getUserItems_validUser_returnsOwnedItems() {
        Purchase purchase = mock(Purchase.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findAllByUser_Id(userId))
                .thenReturn(List.of(purchase));

        when(purchase.getProduct()).thenReturn(product);
        when(purchase.getPricePaid()).thenReturn(30);
        when(purchase.isEquipped()).thenReturn(true);

        List<OwnedItemDto> result =
                purchaseService.getUserItems(email);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(purchaseRepository).findAllByUser_Id(userId);
    }

    @Test
    void getUserItems_noItems_returnsEmptyList() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findAllByUser_Id(userId))
                .thenReturn(List.of());

        List<OwnedItemDto> result =
                purchaseService.getUserItems(email);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserItems_userNotFound_throwsNotFound() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.getUserItems(email)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(purchaseRepository);
    }

    // -------------------------------------------------------
    // SET EQUIPPED
    // -------------------------------------------------------

    @Test
    void setEquipped_ownedItem_equipsItem() {
        Purchase purchase = mock(Purchase.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.of(purchase));

        when(purchase.getProduct()).thenReturn(product);
        when(purchase.getPricePaid()).thenReturn(30);
        when(purchase.isEquipped()).thenReturn(true);

        when(purchaseRepository.save(purchase)).thenReturn(purchase);

        OwnedItemDto result =
                purchaseService.setEquipped(email, productId, true);

        assertNotNull(result);

        verify(purchase).setEquipped(true);
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void setEquipped_unequipsItem() {
        Purchase purchase = mock(Purchase.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.of(purchase));

        when(purchase.getProduct()).thenReturn(product);
        when(purchase.getPricePaid()).thenReturn(30);
        when(purchase.isEquipped()).thenReturn(false);

        when(purchaseRepository.save(purchase)).thenReturn(purchase);

        OwnedItemDto result =
                purchaseService.setEquipped(email, productId, false);

        assertNotNull(result);

        verify(purchase).setEquipped(false);
        verify(purchaseRepository).save(purchase);

        verify(purchaseRepository, never())
                .findAllByUser_IdAndProduct_Category(any(), any());
    }

    @Test
    void setEquipped_equippingItem_unequipsOtherItemsInSameCategory() {
        Purchase purchase = mock(Purchase.class);
        Purchase otherPurchase = mock(Purchase.class);

        UUID purchaseId = UUID.randomUUID();
        UUID otherPurchaseId = UUID.randomUUID();

        Product.Category category = Product.Category.values()[0];

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.of(purchase));

        when(purchase.getId()).thenReturn(purchaseId);
        when(purchase.getProduct()).thenReturn(product);
        when(purchase.getPricePaid()).thenReturn(30);
        when(purchase.isEquipped()).thenReturn(true);

        when(product.getCategory()).thenReturn(category);

        when(purchaseRepository.findAllByUser_IdAndProduct_Category(
                userId, category
        )).thenReturn(List.of(purchase, otherPurchase));

        when(otherPurchase.getId()).thenReturn(otherPurchaseId);
        when(otherPurchase.isEquipped()).thenReturn(true);

        when(purchaseRepository.save(any(Purchase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OwnedItemDto result =
                purchaseService.setEquipped(email, productId, true);

        assertNotNull(result);

        verify(otherPurchase).setEquipped(false);
        verify(purchaseRepository).save(otherPurchase);

        verify(purchase).setEquipped(true);
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void setEquipped_otherItemsAlreadyUnequipped_doesNotChangeThem() {
        Purchase purchase = mock(Purchase.class);
        Purchase otherPurchase = mock(Purchase.class);

        UUID purchaseId = UUID.randomUUID();

        Product.Category category = Product.Category.values()[0];

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.of(purchase));

        when(purchase.getId()).thenReturn(purchaseId);
        when(purchase.getProduct()).thenReturn(product);
        when(purchase.getPricePaid()).thenReturn(30);
        when(purchase.isEquipped()).thenReturn(true);

        when(product.getCategory()).thenReturn(category);

        when(purchaseRepository.findAllByUser_IdAndProduct_Category(
                userId, category
        )).thenReturn(List.of(purchase, otherPurchase));

        when(otherPurchase.getId()).thenReturn(UUID.randomUUID());
        when(otherPurchase.isEquipped()).thenReturn(false);

        when(purchaseRepository.save(purchase)).thenReturn(purchase);

        OwnedItemDto result =
                purchaseService.setEquipped(email, productId, true);

        assertNotNull(result);

        verify(otherPurchase, never()).setEquipped(false);
        verify(purchaseRepository, never()).save(otherPurchase);
        verify(purchase).setEquipped(true);
    }

    @Test
    void setEquipped_itemNotOwned_throwsNotFound() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(purchaseRepository.findByUser_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.setEquipped(email, productId, true)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void setEquipped_userNotFound_throwsNotFound() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.setEquipped(email, productId, true)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(purchaseRepository);
    }
}
