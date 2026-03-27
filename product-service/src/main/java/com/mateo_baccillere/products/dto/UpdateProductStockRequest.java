package com.mateo_baccillere.products.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateProductStockRequest {

    @NotNull
    @Min(0)
    private Integer stock;


    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
