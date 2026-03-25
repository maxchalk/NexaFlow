package com.nexaflow.productservice.service;

import com.nexaflow.productservice.dto.CreateProductRequest;
import com.nexaflow.productservice.dto.ProductDto;
import com.nexaflow.productservice.model.Product;
import com.nexaflow.productservice.model.ProductEvent;
import com.nexaflow.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductDto createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();
        return toDto(productRepository.save(product));
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toDto(product);
    }

    public ProductDto updateStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setStockQuantity(quantity);
        product = productRepository.save(product);

        String eventType;
        if (quantity == 0) {
            eventType = "OUT_OF_STOCK";
        } else if (quantity < 5) {
            eventType = "STOCK_LOW";
        } else {
            eventType = "STOCK_UPDATED";
        }

        try {
            ProductEvent event = new ProductEvent(eventType, product.getId(), product.getName(), quantity, LocalDateTime.now());
            kafkaTemplate.send("product-events", String.valueOf(product.getId()), event);
            log.info("Published {} event for productId={}", eventType, product.getId());
        } catch (Exception e) {
            log.warn("Failed to publish product event: {}", e.getMessage());
        }

        return toDto(product);
    }

    public List<ProductDto> searchProducts(String keyword, String category) {
        List<Product> products;
        if (keyword != null && !keyword.isBlank() && category != null && !category.isBlank()) {
            products = productRepository.findByActiveTrueAndNameContainingIgnoreCaseAndCategory(keyword, category);
        } else if (keyword != null && !keyword.isBlank()) {
            products = productRepository.findByActiveTrueAndNameContainingIgnoreCase(keyword);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByActiveTrueAndCategory(category);
        } else {
            products = productRepository.findByActiveTrueOrderByCreatedAtDesc();
        }
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ProductDto> getFeaturedProducts() {
        return productRepository.findTop8ByActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    private ProductDto toDto(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .category(p.getCategory())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
