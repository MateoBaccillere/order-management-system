package com.mateo_baccillere.products.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Entity
@Table(name = "products")
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, name = "seller_id")
    private Long sellerId;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean active;


    public Product() {
    }

    public Product(Long id, String name, String description, BigDecimal price, Long sellerId, Integer stock, Boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.sellerId = sellerId;
        this.stock = stock;
        this.active = active;
    }

    public Product(String trim, String s, BigDecimal price, Long sellerId, Integer stock, Boolean active) {
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}
