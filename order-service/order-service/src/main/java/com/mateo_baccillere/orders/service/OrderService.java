package com.mateo_baccillere.orders.service;

import com.mateo_baccillere.orders.dto.CreateOrderItemRequest;
import com.mateo_baccillere.orders.dto.CreateOrderRequest;
import com.mateo_baccillere.orders.dto.OrderItemResponse;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderItem;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.exception.OrderNotFoundException;
import com.mateo_baccillere.orders.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setStatus(OrderStatus.CREATED);

        List<OrderItem> items = request.getItems()
                .stream()
                .map(itemRequest -> mapToOrderItem(itemRequest, order))
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OrderItem mapToOrderItem(CreateOrderItemRequest request, Order order) {
        OrderItem item = new OrderItem();
        item.setProductName(request.getProductName());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());

        BigDecimal subtotal = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        item.setSubtotal(subtotal);
        item.setOrder(order);

        return item;
    }

    private OrderResponse mapToResponse(Order order) {
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
