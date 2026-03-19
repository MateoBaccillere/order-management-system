package com.mateo_baccillere.orders.service;

import com.mateo_baccillere.orders.client.NotificationClient;
import com.mateo_baccillere.orders.dto.*;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderItem;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.exception.InvalidOrderStateTransitionException;
import com.mateo_baccillere.orders.exception.OrderNotFoundException;
import com.mateo_baccillere.orders.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository orderRepository, NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.notificationClient = notificationClient;
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

    @Transactional
    public OrderResponse confirmOrder(Long id) {
        Order order = getOrderOrThrow(id);

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateTransitionException(
                    "Order is already confirmed"
            );
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateTransitionException(
                    "Only orders in CREATED status can be confirmed"
            );
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        notificationClient.sendNotification(
                new NotificationRequest(
                        savedOrder.getId(),
                        "ORDER_CONFIRMED",
                        "Order " + savedOrder.getId() + " was confirmed"
                )
        );

        return mapToResponse(savedOrder);



    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = getOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateTransitionException(
                    "Only orders in CREATED or CONFIRMED status can be cancelled"
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        notificationClient.sendNotification(
                new NotificationRequest(
                        savedOrder.getId(),
                        "ORDER_CANCELLED",
                        "Order " + savedOrder.getId() + " was cancelled"
                )
        );

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse shipOrder(Long id) {
        Order order = getOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateTransitionException(
                    "Only orders in CONFIRMED status can be shipped"
            );
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order savedOrder = orderRepository.save(order);

        notificationClient.sendNotification(
                new NotificationRequest(
                        savedOrder.getId(),
                        "ORDER_SHIPPED",
                        "Order " + savedOrder.getId() + " was shipped"
                )
        );

        return mapToResponse(savedOrder);
    }

    public void deleteOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        orderRepository.delete(order);

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

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }



}
