package com.mateo_baccillere.products.repository;

import com.mateo_baccillere.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    boolean existsByNameIgnoreCase(String name);
}
