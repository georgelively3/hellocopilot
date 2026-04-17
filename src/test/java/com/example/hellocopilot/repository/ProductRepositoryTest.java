package com.example.hellocopilot.repository;

import com.example.hellocopilot.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("ProductRepository Integration Tests")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        savedProduct = productRepository.save(Product.builder()
                .name("Widget A")
                .description("A useful widget")
                .price(new BigDecimal("9.99"))
                .quantity(100)
                .build());
    }

    @Test
    @DisplayName("save persists a product and generates id")
    void save_persistsProduct() {
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Widget A");
    }

    @Test
    @DisplayName("findById returns present optional for existing product")
    void findById_returnsProduct() {
        Optional<Product> result = productRepository.findById(savedProduct.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Widget A");
    }

    @Test
    @DisplayName("findById returns empty optional for non-existing id")
    void findById_returnsEmpty() {
        Optional<Product> result = productRepository.findById(9999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAll returns all persisted products")
    void findAll_returnsAllProducts() {
        productRepository.save(Product.builder()
                .name("Widget B")
                .description("Another widget")
                .price(new BigDecimal("14.99"))
                .quantity(50)
                .build());

        List<Product> result = productRepository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase returns partial case-insensitive matches")
    void findByNameContainingIgnoreCase_returnsMatches() {
        List<Product> result = productRepository.findByNameContainingIgnoreCase("widget");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Widget A");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase returns empty when no match")
    void findByNameContainingIgnoreCase_returnsEmpty() {
        List<Product> result = productRepository.findByNameContainingIgnoreCase("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByName returns true for existing name")
    void existsByName_returnsTrue() {
        assertThat(productRepository.existsByName("Widget A")).isTrue();
    }

    @Test
    @DisplayName("existsByName returns false for missing name")
    void existsByName_returnsFalse() {
        assertThat(productRepository.existsByName("Unknown")).isFalse();
    }

    @Test
    @DisplayName("deleteById removes product")
    void deleteById_removesProduct() {
        productRepository.deleteById(savedProduct.getId());

        assertThat(productRepository.findById(savedProduct.getId())).isEmpty();
    }

    @Test
    @DisplayName("existsById returns true for existing product")
    void existsById_returnsTrue() {
        assertThat(productRepository.existsById(savedProduct.getId())).isTrue();
    }

    @Test
    @DisplayName("existsById returns false for non-existing product")
    void existsById_returnsFalse() {
        assertThat(productRepository.existsById(9999L)).isFalse();
    }
}
