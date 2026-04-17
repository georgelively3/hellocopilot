package com.example.hellocopilot.service;

import com.example.hellocopilot.dto.ProductRequest;
import com.example.hellocopilot.dto.ProductResponse;
import com.example.hellocopilot.exception.ProductNotFoundException;
import com.example.hellocopilot.model.Product;
import com.example.hellocopilot.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("19.99"))
                .quantity(10)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = ProductRequest.builder()
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("19.99"))
                .quantity(10)
                .build();
    }

    // ---- getAllProducts ----

    @Test
    @DisplayName("getAllProducts returns list of responses")
    void getAllProducts_returnsMappedList() {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct));

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Product");
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("getAllProducts returns empty list when no products")
    void getAllProducts_returnsEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result).isEmpty();
    }

    // ---- getProductById ----

    @Test
    @DisplayName("getProductById returns response for valid id")
    void getProductById_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualByComparingTo("19.99");
    }

    @Test
    @DisplayName("getProductById throws ProductNotFoundException for missing id")
    void getProductById_throwsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---- searchProductsByName ----

    @Test
    @DisplayName("searchProductsByName returns matching products")
    void searchProductsByName_returnsMatches() {
        when(productRepository.findByNameContainingIgnoreCase("test"))
                .thenReturn(List.of(sampleProduct));

        List<ProductResponse> result = productService.searchProductsByName("test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("searchProductsByName returns empty list when no matches")
    void searchProductsByName_returnsEmpty() {
        when(productRepository.findByNameContainingIgnoreCase("xyz")).thenReturn(List.of());

        List<ProductResponse> result = productService.searchProductsByName("xyz");

        assertThat(result).isEmpty();
    }

    // ---- createProduct ----

    @Test
    @DisplayName("createProduct saves and returns response")
    void createProduct_savesAndReturnsResponse() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse result = productService.createProduct(sampleRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getQuantity()).isEqualTo(10);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct maps all fields correctly")
    void createProduct_mapsAllFields() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse result = productService.createProduct(sampleRequest);

        assertThat(result.getDescription()).isEqualTo("A test product");
        assertThat(result.getPrice()).isEqualByComparingTo("19.99");
    }

    // ---- updateProduct ----

    @Test
    @DisplayName("updateProduct modifies existing product")
    void updateProduct_updatesAndReturnsResponse() {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("Updated Name")
                .description("Updated desc")
                .price(new BigDecimal("29.99"))
                .quantity(20)
                .build();

        Product updatedProduct = Product.builder()
                .id(1L)
                .name("Updated Name")
                .description("Updated desc")
                .price(new BigDecimal("29.99"))
                .quantity(20)
                .createdAt(sampleProduct.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        ProductResponse result = productService.updateProduct(1L, updateRequest);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getPrice()).isEqualByComparingTo("29.99");
        assertThat(result.getQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("updateProduct throws ProductNotFoundException when product missing")
    void updateProduct_throwsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, sampleRequest))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).save(any());
    }

    // ---- deleteProduct ----

    @Test
    @DisplayName("deleteProduct removes existing product")
    void deleteProduct_deletesSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        assertThatCode(() -> productService.deleteProduct(1L)).doesNotThrowAnyException();

        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProduct throws ProductNotFoundException when product missing")
    void deleteProduct_throwsWhenNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).deleteById(any());
    }
}
