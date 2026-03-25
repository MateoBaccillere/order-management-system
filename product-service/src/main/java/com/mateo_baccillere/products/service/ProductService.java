package com.mateo_baccillere.products.service;

import com.mateo_baccillere.products.dto.CreateProductRequest;
import com.mateo_baccillere.products.dto.ProductResponse;
import com.mateo_baccillere.products.entity.Product;
import com.mateo_baccillere.products.exception.ProductNotFoundException;
import com.mateo_baccillere.products.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }


    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product(
                request.getName(),
                request.getDescription(),
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStock(stock);
        return mapToResponse(productRepository.save(product));
    }

    public ProductResponse updateStatus(Long id, Boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(active);
        return mapToResponse(productRepository.save(product));
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
