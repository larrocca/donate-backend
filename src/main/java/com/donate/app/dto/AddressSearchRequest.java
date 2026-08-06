package com.donate.app.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressSearchRequest(
        String nombreInstitucion,
        @NotBlank(message = "provincia is required")
        String provincia,
        @NotBlank(message = "localidad is required")
        String localidad,
        @NotBlank(message = "calle is required")
        String calle,
        @NotBlank(message = "numeracion is required")
        String numeracion
) {
}
