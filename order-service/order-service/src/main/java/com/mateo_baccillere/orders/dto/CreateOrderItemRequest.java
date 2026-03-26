package com.mateo_baccillere.orders.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderItemRequest {

    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity;

}
