package org.mateo_baccillere.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.mateo_baccillere.entity.UserRole;

@Getter
@AllArgsConstructor
public class UserRoleResponse {

    private UserRole role;
}
