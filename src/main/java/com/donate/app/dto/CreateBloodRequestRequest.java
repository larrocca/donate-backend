package com.donate.app.dto;

import com.donate.app.model.BloodType;

import java.math.BigDecimal;

public record CreateBloodRequestRequest(
        BloodType bloodType,
        Long provinciaId,
        Long localidadId,
        String nombreInstitucion,
        String calle,
        String numeracion,
        String description,
        String direccionFormateada,
        BigDecimal latitud,
        BigDecimal longitud,
        String ubicacionExternalId
) {
}
