package com.financemanager.dto;

import jakarta.validation.constraints.NotBlank;

public class LinkRequestDto {

    @NotBlank(message = "Student email or invitation code is required")
    private String identifier;

    public LinkRequestDto() {}

    public LinkRequestDto(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
}
