package com.mateo_baccillere.orders.repository;

import com.mateo_baccillere.orders.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,Long> {


    Optional<Cart> findByCustomerName(String customerName);
}
