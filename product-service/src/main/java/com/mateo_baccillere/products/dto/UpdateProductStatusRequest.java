package com.mateo_baccillere.products.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateProductStatusRequest {

    @NotNull
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
