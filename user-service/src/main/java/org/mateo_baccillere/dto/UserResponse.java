package org.mateo_baccillere.dto;


import org.mateo_baccillere.entity.User;
import lombok.Builder;
import lombok.Getter;
import org.mateo_baccillere.entity.UserRole;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
