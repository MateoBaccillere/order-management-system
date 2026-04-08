package com.mateo_baccillere.products.dto;



import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Long sellerId;
    private Integer stock;
    private Boolean active;

    public ProductResponse(Long id, String name, String description, BigDecimal price, Long sellerId, Integer stock, Boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.sellerId = sellerId;
        this.stock = stock;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public Boolean getActive() {
        return active;
    }
}
