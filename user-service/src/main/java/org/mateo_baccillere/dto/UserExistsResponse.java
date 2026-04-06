package org.mateo_baccillere.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserExistsResponse {
    private boolean exists;

}
