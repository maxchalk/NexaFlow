package com.nexaflow.productservice.repository;

import com.nexaflow.productservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    List<Product> findByActiveTrueOrderByCreatedAtDesc();
    List<Product> findTop8ByActiveTrueOrderByCreatedAtDesc();
    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);
    List<Product> findByActiveTrueAndCategory(String category);
    List<Product> findByActiveTrueAndNameContainingIgnoreCaseAndCategory(String name, String category);
    Optional<Product> findByIdAndActiveTrue(Long id);
}
