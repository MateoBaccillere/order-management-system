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
    private final CatalogProductValidationService catalogProductValidationService;
    private final OrderCreationService orderCreationService;

    public OrderService(
            OrderRepository orderRepository,
            NotificationClient notificationClient,
            CatalogProductValidationService catalogProductValidationService,
            OrderCreationService orderCreationService
    ) {
        this.orderRepository = orderRepository;
        this.notificationClient = notificationClient;
        this.catalogProductValidationService = catalogProductValidationService;
        this.orderCreationService = orderCreationService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<ValidatedProductSnapshot> validatedItems = request.getItems()
                .stream()
                .map(item -> catalogProductValidationService.validateAndSnapshot(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        return orderCreationService.createOrderFromValidatedItems(
                request.getCustomerName(),
                validatedItems
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = getOrderOrThrow(id);
        return orderCreationService.mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderCreationService::mapToResponse)
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

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        notificationClient.sendNotification(
                new NotificationRequest(
                        savedOrder.getId(),
                        "ORDER_CONFIRMED",
                        "Order " + savedOrder.getId() + " was confirmed"
                )
        );

        return orderCreationService.mapToResponse(savedOrder);
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

        return orderCreationService.mapToResponse(savedOrder);
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

        return orderCreationService.mapToResponse(savedOrder);
    }

    @Transactional
    public void deleteOrderById(Long id) {
        Order order = getOrderOrThrow(id);
        orderRepository.delete(order);
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }



}
