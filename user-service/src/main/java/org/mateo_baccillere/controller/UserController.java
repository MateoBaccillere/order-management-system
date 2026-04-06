package org.mateo_baccillere.controller;


import org.mateo_baccillere.dto.CreateUserRequest;
import org.mateo_baccillere.dto.UserExistsResponse;
import org.mateo_baccillere.dto.UserResponse;
import org.mateo_baccillere.dto.UserRoleResponse;
import org.mateo_baccillere.entity.User;
import org.mateo_baccillere.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.getName(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(userService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = userService.getAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<UserExistsResponse> exists(@PathVariable Long id) {
        return ResponseEntity.ok(new UserExistsResponse(userService.existsById(id)));
    }

    @GetMapping("/{id}/role")
    public ResponseEntity<UserRoleResponse> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(new UserRoleResponse(userService.getRoleById(id)));
    }

    @PatchMapping("/{id}/promote-to-seller")
    public ResponseEntity<UserResponse> promoteToSeller(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(userService.promoteToSeller(id)));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
