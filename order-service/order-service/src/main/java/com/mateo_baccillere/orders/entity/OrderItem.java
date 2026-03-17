package com.mateo_baccillere.orders.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_item")
public class OrderItem {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;
    private int quantity;
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

}
