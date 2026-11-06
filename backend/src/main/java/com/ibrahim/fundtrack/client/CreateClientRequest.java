package com.ibrahim.fundtrack.client;

import jakarta.validation.constraints.*;

public record CreateClientRequest(
        @NotBlank @Size(max = 100) String name,

        @NotBlank @Pattern(regexp = "SA-\\d{6}") @Size(max = 20) String subaccountNumber) {
}
