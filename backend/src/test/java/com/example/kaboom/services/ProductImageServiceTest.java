
package com.example.kaboom.services;

import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.ProductImage;
import com.example.kaboom.repositories.ProductImageRepository;
import com.example.kaboom.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductImageService productImageService;

    private UUID productId;
    private Product product;

    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = mock(Product.class);

        validFile = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                new byte[]{1, 2, 3, 4, 5}
        );
    }

    // -------------------------------------------------------
    // UPLOAD IMAGE
    // -------------------------------------------------------

    @Test
    void uploadImage_validFile_savesNewImage() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.empty());

        productImageService.uploadImage(productId, validFile);

        ArgumentCaptor<ProductImage> captor =
                ArgumentCaptor.forClass(ProductImage.class);

        verify(productImageRepository).save(captor.capture());

        ProductImage savedImage = captor.getValue();

        assertSame(product, savedImage.getProduct());
        assertArrayEquals(
                new byte[]{1, 2, 3, 4, 5},
                savedImage.getImageData()
        );
        assertEquals("image/png", savedImage.getContentType());
        assertEquals(validFile.getSize(), savedImage.getFileSize());

        verify(productRepository).findById(productId);
    }

    @Test
    void uploadImage_existingImage_replacesImageData() {
        ProductImage existingImage = mock(ProductImage.class);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.of(existingImage));

        productImageService.uploadImage(productId, validFile);

        verify(existingImage).setProduct(product);
        verify(existingImage).setImageData(
                new byte[]{1, 2, 3, 4, 5}
        );
        verify(existingImage).setContentType("image/png");
        verify(existingImage).setFileSize(validFile.getSize());

        verify(productImageRepository).save(existingImage);
    }

    @Test
    void uploadImage_nullFile_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.uploadImage(productId, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(productRepository, productImageRepository);
    }

    @Test
    void uploadImage_emptyFile_throwsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.uploadImage(productId, emptyFile)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(productRepository, productImageRepository);
    }

    @Test
    void uploadImage_fileTooLarge_throwsPayloadTooLarge() {
        MultipartFile largeFile = mock(MultipartFile.class);

        when(largeFile.isEmpty()).thenReturn(false);
        when(largeFile.getSize()).thenReturn(5L * 1024 * 1024 + 1);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.uploadImage(productId, largeFile)
        );

        assertEquals(
                HttpStatus.PAYLOAD_TOO_LARGE,
                exception.getStatusCode()
        );

        verifyNoInteractions(productRepository, productImageRepository);
    }

    @Test
    void uploadImage_nullContentType_throwsUnsupportedMediaType() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.uploadImage(productId, file)
        );

        assertEquals(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                exception.getStatusCode()
        );

        verifyNoInteractions(productRepository, productImageRepository);
    }

    @Test
    void uploadImage_unsupportedContentType_throwsUnsupportedMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.gif",
                "image/gif",
                new byte[]{1, 2, 3}
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.uploadImage(productId, file)
        );

        assertEquals(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                exception.getStatusCode()
        );

        verifyNoInteractions(productRepository, productImageRepository);
    }

    @Test
    void uploadImage_productNotFound_throwsNotFound() {
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.uploadImage(productId, validFile)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productImageRepository, never())
                .findByProductId(any(UUID.class));

        verify(productImageRepository, never())
                .save(any(ProductImage.class));
    }

    @Test
    void uploadImage_uppercaseContentType_savesLowercaseContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "IMAGE/PNG",
                new byte[]{1, 2, 3}
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.empty());

        productImageService.uploadImage(productId, file);

        ArgumentCaptor<ProductImage> captor =
                ArgumentCaptor.forClass(ProductImage.class);

        verify(productImageRepository).save(captor.capture());

        assertEquals("image/png", captor.getValue().getContentType());
    }

    @Test
    void uploadImage_fileReadFails_throwsInternalServerError() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.empty());
        when(file.getBytes()).thenThrow(new IOException("Read failed"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.uploadImage(productId, file)
        );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode()
        );

        verify(productImageRepository, never())
                .save(any(ProductImage.class));
    }

    // -------------------------------------------------------
    // GET IMAGE
    // -------------------------------------------------------

    @Test
    void getImage_imageExists_returnsImage() {
        ProductImage image = mock(ProductImage.class);

        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.of(image));

        ProductImage result = productImageService.getImage(productId);

        assertNotNull(result);
        assertSame(image, result);

        verify(productImageRepository).findByProductId(productId);
    }

    @Test
    void getImage_imageNotFound_throwsNotFound() {
        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.getImage(productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productImageRepository).findByProductId(productId);
    }

    // -------------------------------------------------------
    // DELETE IMAGE
    // -------------------------------------------------------

    @Test
    void deleteImage_productAndImageExist_deletesImage() {
        ProductImage image = mock(ProductImage.class);

        when(productRepository.existsById(productId))
                .thenReturn(true);

        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.of(image));

        productImageService.deleteImage(productId);

        verify(productRepository).existsById(productId);
        verify(productImageRepository).findByProductId(productId);
        verify(productImageRepository).deleteByProductId(productId);
    }

    @Test
    void deleteImage_productNotFound_throwsNotFound() {
        when(productRepository.existsById(productId))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.deleteImage(productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productImageRepository, never())
                .findByProductId(any(UUID.class));

        verify(productImageRepository, never())
                .deleteByProductId(any(UUID.class));
    }

    @Test
    void deleteImage_imageNotFound_throwsNotFound() {
        when(productRepository.existsById(productId))
                .thenReturn(true);

        when(productImageRepository.findByProductId(productId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productImageService.deleteImage(productId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productImageRepository, never())
                .deleteByProductId(any(UUID.class));
    }
}

