package com.example.hellocopilot.controller;

import com.example.hellocopilot.dto.ProductRequest;
import com.example.hellocopilot.dto.ProductResponse;
import com.example.hellocopilot.exception.GlobalExceptionHandler;
import com.example.hellocopilot.exception.ProductNotFoundException;
import com.example.hellocopilot.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { ProductController.class, GlobalExceptionHandler.class })
@DisplayName("ProductController Unit Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponse sampleResponse;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = ProductResponse.builder()
                .id(1L)
                .name("Laptop Pro")
                .description("High-performance laptop")
                .price(new BigDecimal("1299.99"))
                .quantity(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = ProductRequest.builder()
                .name("Laptop Pro")
                .description("High-performance laptop")
                .price(new BigDecimal("1299.99"))
                .quantity(50)
                .build();
    }

    // ---- GET /api/products ----

    @Test
    @DisplayName("GET /api/products returns 200 with list")
    void getAllProducts_returns200() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop Pro"));
    }

    @Test
    @DisplayName("GET /api/products returns empty list when no products")
    void getAllProducts_returnsEmptyList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- GET /api/products/{id} ----

    @Test
    @DisplayName("GET /api/products/{id} returns 200 for existing product")
    void getProductById_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(1299.99));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 when not found")
    void getProductById_returns404() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    // ---- GET /api/products/search ----

    @Test
    @DisplayName("GET /api/products/search returns matching products")
    void searchByName_returns200() throws Exception {
        when(productService.searchProductsByName("Laptop")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/products/search").param("name", "Laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop Pro"));
    }

    @Test
    @DisplayName("GET /api/products/search returns empty list when no match")
    void searchByName_returnsEmpty() throws Exception {
        when(productService.searchProductsByName("xyz")).thenReturn(List.of());

        mockMvc.perform(get("/api/products/search").param("name", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- POST /api/products ----

    @Test
    @DisplayName("POST /api/products returns 201 with created product")
    void createProduct_returns201() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop Pro"));
    }

    @Test
    @DisplayName("POST /api/products returns 400 when name is blank")
    void createProduct_returns400_whenNameBlank() throws Exception {
        sampleRequest.setName("");

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @DisplayName("POST /api/products returns 400 when price is null")
    void createProduct_returns400_whenPriceNull() throws Exception {
        sampleRequest.setPrice(null);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    @DisplayName("POST /api/products returns 400 when quantity is null")
    void createProduct_returns400_whenQuantityNull() throws Exception {
        sampleRequest.setQuantity(null);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    // ---- PUT /api/products/{id} ----

    @Test
    @DisplayName("PUT /api/products/{id} returns 200 with updated product")
    void updateProduct_returns200() throws Exception {
        ProductResponse updated = ProductResponse.builder()
                .id(1L)
                .name("Updated Laptop")
                .description("Updated description")
                .price(new BigDecimal("999.99"))
                .quantity(30)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(updated);

        sampleRequest.setName("Updated Laptop");
        sampleRequest.setPrice(new BigDecimal("999.99"));
        sampleRequest.setQuantity(30);

        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(999.99));
    }

    @Test
    @DisplayName("PUT /api/products/{id} returns 404 when product not found")
    void updateProduct_returns404() throws Exception {
        when(productService.updateProduct(eq(99L), any(ProductRequest.class)))
                .thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(put("/api/products/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} returns 400 on invalid body")
    void updateProduct_returns400_onInvalidBody() throws Exception {
        sampleRequest.setName(null);

        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest());
    }

    // ---- DELETE /api/products/{id} ----

    @Test
    @DisplayName("DELETE /api/products/{id} returns 204 when deleted")
    void deleteProduct_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} returns 404 when not found")
    void deleteProduct_returns404() throws Exception {
        doThrow(new ProductNotFoundException(99L)).when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }
}
