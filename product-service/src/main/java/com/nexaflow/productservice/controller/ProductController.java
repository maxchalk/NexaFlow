package com.nexaflow.productservice.controller;

import com.nexaflow.productservice.dto.CreateProductRequest;
import com.nexaflow.productservice.dto.ProductDto;
import com.nexaflow.productservice.dto.UpdateStockRequest;
import com.nexaflow.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog and inventory management")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product (ADMIN only)")
    public ResponseEntity<ProductDto> createProduct(
            @RequestBody CreateProductRequest request,
            @RequestHeader(value = "X-User-Role", defaultValue = "CUSTOMER") String userRole) {
        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping
    @Operation(summary = "Get all active products")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by keyword and/or category")
    public ResponseEntity<List<ProductDto>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(productService.searchProducts(keyword, category));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products (top 8 newest)")
    public ResponseEntity<List<ProductDto>> getFeaturedProducts() {
        return ResponseEntity.ok(productService.getFeaturedProducts());
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update product stock quantity (ADMIN only)")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable Long id,
            @RequestBody UpdateStockRequest request,
            @RequestHeader(value = "X-User-Role", defaultValue = "CUSTOMER") String userRole) {
        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(productService.updateStock(id, request.getQuantity()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a product (ADMIN only)")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", defaultValue = "CUSTOMER") String userRole) {
        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deactivated successfully"));
    }
}
