package com.mateo_baccillere.orders.controller;


import com.mateo_baccillere.orders.dto.AddCartItemRequest;
import com.mateo_baccillere.orders.dto.CartResponse;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.dto.UpdateCartItemQuantityRequest;
import com.mateo_baccillere.orders.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {


    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{customerName}")
    public ResponseEntity<CartResponse> getCart(@PathVariable String customerName) {
        return ResponseEntity.ok(cartService.getCart(customerName));
    }

    @PostMapping("/{customerName}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable String customerName,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ResponseEntity.ok(cartService.addItem(customerName, request));
    }

    @PatchMapping("/{customerName}/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable String customerName,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        return ResponseEntity.ok(cartService.updateItemQuantity(customerName, productId, request));
    }

    @DeleteMapping("/{customerName}/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable String customerName,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(cartService.removeItem(customerName, productId));
    }

    @DeleteMapping("/{customerName}/items")
    public ResponseEntity<CartResponse> clearCart(@PathVariable String customerName) {
        return ResponseEntity.ok(cartService.clearCart(customerName));
    }

    @PostMapping("/{customerName}/checkout")
    public ResponseEntity<OrderResponse> checkout(@PathVariable String customerName) {
        return ResponseEntity.ok(cartService.checkout(customerName));
    }
}
