package com.mateo_baccillere.products.service;

import com.mateo_baccillere.products.dto.CreateProductRequest;
import com.mateo_baccillere.products.dto.ProductResponse;
import com.mateo_baccillere.products.entity.Product;
import com.mateo_baccillere.products.exception.BusinessException;
import com.mateo_baccillere.products.exception.ProductNotFoundException;
import com.mateo_baccillere.products.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(CreateProductRequest request) {
        validateCreateRequest(request);

        Product product = new Product(
                request.getName().trim(),
                normalizeDescription(request.getDescription()),
                request.getPrice(),
                request.getStock(),
                request.getActive()
        );

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return mapToResponse(product);
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse updateStock(Long id, Integer stock) {
        validateStock(stock);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStock(stock);
        Product updated = productRepository.save(product);

        return mapToResponse(updated);
    }

    public ProductResponse updateStatus(Long id, Boolean active) {
        if (active == null) {
            throw new BusinessException("Product active status cannot be null");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(active);
        Product updated = productRepository.save(product);

        return mapToResponse(updated);
    }

    private void validateCreateRequest(CreateProductRequest request) {
        validateName(request.getName());
        validatePrice(request.getPrice());
        validateStock(request.getStock());

        if (request.getActive() == null) {
            throw new BusinessException("Product active status cannot be null");
        }

        if (productRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BusinessException("Product name already exists");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("Product name cannot be blank");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Product price must be greater than zero");
        }
    }

    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new BusinessException("Product stock cannot be negative");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getActive()
        );
    }
}
