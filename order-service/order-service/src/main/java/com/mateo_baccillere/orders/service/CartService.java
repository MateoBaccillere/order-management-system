package com.mateo_baccillere.orders.service;

import com.mateo_baccillere.orders.dto.AddCartItemRequest;
import com.mateo_baccillere.orders.dto.CartItemResponse;
import com.mateo_baccillere.orders.dto.CartResponse;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.dto.UpdateCartItemQuantityRequest;
import com.mateo_baccillere.orders.entity.Cart;
import com.mateo_baccillere.orders.entity.CartItem;
import com.mateo_baccillere.orders.exception.CartItemNotFoundException;
import com.mateo_baccillere.orders.exception.CartNotFoundException;
import com.mateo_baccillere.orders.exception.EmptyCartCheckoutException;
import com.mateo_baccillere.orders.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CatalogProductValidationService catalogProductValidationService;
    private final OrderCreationService orderCreationService;

    public CartService(
            CartRepository cartRepository,
            CatalogProductValidationService catalogProductValidationService,
            OrderCreationService orderCreationService
    ) {
        this.cartRepository = cartRepository;
        this.catalogProductValidationService = catalogProductValidationService;
        this.orderCreationService = orderCreationService;
    }

    @Transactional
    public CartResponse getCart(String customerName) {
        Cart cart = getOrCreateCart(customerName);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String customerName, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(customerName);

        CartItem existingItem = cart.getItems()
                .stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        int targetQuantity = request.getQuantity();
        if (existingItem != null) {
            targetQuantity = existingItem.getQuantity() + request.getQuantity();
        }

        ValidatedProductSnapshot snapshot =
                catalogProductValidationService.validateAndSnapshot(request.getProductId(), targetQuantity);

        if (existingItem == null) {
            CartItem newItem = new CartItem();
            newItem.setProductId(snapshot.getProductId());
            newItem.setProductName(snapshot.getProductName());
            newItem.setQuantity(snapshot.getQuantity());
            newItem.setUnitPrice(snapshot.getUnitPrice());
            newItem.setSubtotal(snapshot.getSubtotal());
            newItem.setCart(cart);

            cart.getItems().add(newItem);
        } else {
            existingItem.setProductName(snapshot.getProductName());
            existingItem.setQuantity(snapshot.getQuantity());
            existingItem.setUnitPrice(snapshot.getUnitPrice());
            existingItem.setSubtotal(snapshot.getSubtotal());
        }

        recalculateCartTotal(cart);
        Cart savedCart = cartRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public CartResponse updateItemQuantity(
            String customerName,
            Long productId,
            UpdateCartItemQuantityRequest request
    ) {
        Cart cart = getCartOrThrow(customerName);

        CartItem item = cart.getItems()
                .stream()
                .filter(cartItem -> cartItem.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        ValidatedProductSnapshot snapshot =
                catalogProductValidationService.validateAndSnapshot(productId, request.getQuantity());

        item.setProductName(snapshot.getProductName());
        item.setQuantity(snapshot.getQuantity());
        item.setUnitPrice(snapshot.getUnitPrice());
        item.setSubtotal(snapshot.getSubtotal());

        recalculateCartTotal(cart);
        Cart savedCart = cartRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public CartResponse removeItem(String customerName, Long productId) {
        Cart cart = getCartOrThrow(customerName);

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new CartItemNotFoundException(productId);
        }

        recalculateCartTotal(cart);
        Cart savedCart = cartRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public CartResponse clearCart(String customerName) {
        Cart cart = getCartOrThrow(customerName);

        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);

        Cart savedCart = cartRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public OrderResponse checkout(String customerName) {
        Cart cart = getCartOrThrow(customerName);

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartCheckoutException(customerName);
        }

        List<ValidatedProductSnapshot> validatedItems = cart.getItems()
                .stream()
                .map(item -> catalogProductValidationService.validateAndSnapshot(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        OrderResponse orderResponse = orderCreationService.createOrderFromValidatedItems(
                customerName,
                validatedItems
        );

        cartRepository.delete(cart);

        return orderResponse;
    }

    private Cart getOrCreateCart(String customerName) {
        return cartRepository.findByCustomerName(customerName)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomerName(customerName);
                    cart.setTotalAmount(BigDecimal.ZERO);
                    return cartRepository.save(cart);
                });
    }

    private Cart getCartOrThrow(String customerName) {
        return cartRepository.findByCustomerName(customerName)
                .orElseThrow(() -> new CartNotFoundException(customerName));
    }

    private void recalculateCartTotal(Cart cart) {
        BigDecimal total = cart.getItems()
                .stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(total);
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new CartResponse(
                cart.getId(),
                cart.getCustomerName(),
                cart.getTotalAmount(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                items
        );
    }
}
