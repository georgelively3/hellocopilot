package com.example.hellocopilot.service;

import com.example.hellocopilot.dto.ProductRequest;
import com.example.hellocopilot.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    List<ProductResponse> searchProductsByName(String name);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
