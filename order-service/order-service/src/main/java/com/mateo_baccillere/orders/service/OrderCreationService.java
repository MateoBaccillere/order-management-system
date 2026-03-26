package com.mateo_baccillere.orders.service;



import com.mateo_baccillere.orders.dto.OrderItemResponse;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderItem;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderCreationService {


    private final OrderRepository orderRepository;

    public OrderCreationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrderFromValidatedItems(String customerName, List<ValidatedProductSnapshot> validatedItems) {
        Order order = buildOrder(customerName, validatedItems);
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional
    public Order createOrderEntityFromValidatedItems(String customerName, List<ValidatedProductSnapshot> validatedItems) {
        Order order = buildOrder(customerName, validatedItems);
        return orderRepository.save(order);
    }

    private Order buildOrder(String customerName, List<ValidatedProductSnapshot> validatedItems) {
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setStatus(OrderStatus.CREATED);

        List<OrderItem> items = validatedItems.stream()
                .map(snapshot -> buildOrderItem(snapshot, order))
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotalAmount(totalAmount);

        return order;
    }

    private OrderItem buildOrderItem(ValidatedProductSnapshot snapshot, Order order) {
        OrderItem item = new OrderItem();
        item.setProductId(snapshot.getProductId());
        item.setProductName(snapshot.getProductName());
        item.setQuantity(snapshot.getQuantity());
        item.setUnitPrice(snapshot.getUnitPrice());
        item.setSubtotal(snapshot.getSubtotal());
        item.setOrder(order);
        return item;
    }

    public OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
