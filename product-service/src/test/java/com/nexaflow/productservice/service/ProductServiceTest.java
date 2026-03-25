package com.nexaflow.productservice.service;

import com.nexaflow.productservice.dto.CreateProductRequest;
import com.nexaflow.productservice.dto.ProductDto;
import com.nexaflow.productservice.model.Product;
import com.nexaflow.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("A test product")
                .category("Electronics")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createProduct_success() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setDescription("A test product");
        request.setCategory("Electronics");
        request.setPrice(new BigDecimal("99.99"));
        request.setStockQuantity(10);

        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDto result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals(new BigDecimal("99.99"), result.getPrice());
    }

    @Test
    void getProductById_notFound_throws() {
        when(productRepository.findByIdAndActiveTrue(anyLong())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(999L));
    }

    @Test
    void updateStock_belowThreshold_publishesEvent() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        productService.updateStock(1L, 3);

        verify(kafkaTemplate).send(eq("product-events"), anyString(), any());
    }
}
