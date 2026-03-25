package com.mateo_baccillere.orders.service;

import com.mateo_baccillere.orders.client.NotificationClient;
import com.mateo_baccillere.orders.client.ProductClient;
import com.mateo_baccillere.orders.dto.*;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderItem;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.exception.InvalidOrderStateTransitionException;
import com.mateo_baccillere.orders.exception.OrderNotFoundException;
import com.mateo_baccillere.orders.exception.ProductUnavailableException;
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
    private final ProductClient productClient;

    public OrderService(
            OrderRepository orderRepository,
            NotificationClient notificationClient,
            ProductClient productClient
    ) {
        this.orderRepository = orderRepository;
        this.notificationClient = notificationClient;
        this.productClient = productClient;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setStatus(OrderStatus.CREATED);

        List<OrderItem> items = request.getItems()
                .stream()
                .map(itemRequest -> buildValidatedOrderItem(itemRequest, order))
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
            throw new InvalidOrderStateTransitionException("Order is already confirmed");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateTransitionException(
                    "Only orders in CREATED status can be confirmed"
            );
        }

        Order savedOrder = orderRepository.save(order);
        savedOrder.setStatus(OrderStatus.CONFIRMED);

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

    private OrderItem buildValidatedOrderItem(CreateOrderItemRequest request, Order order) {
        ProductDetailsResponse product = productClient.getProductById(request.getProductId());

        if (product == null) {
            throw new ProductUnavailableException(
                    "Product not found with id: " + request.getProductId()
            );
        }

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ProductUnavailableException(
                    "Product is inactive with id: " + request.getProductId()
            );
        }

        if (product.getStock() == null || request.getQuantity() > product.getStock()) {
            throw new ProductUnavailableException(
                    "Insufficient stock for product id: " + request.getProductId()
            );
        }

        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(product.getPrice());
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
