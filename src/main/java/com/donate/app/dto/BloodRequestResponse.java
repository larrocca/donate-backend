package com.donate.app.dto;

import com.donate.app.model.BloodRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.BloodType;

import java.math.BigDecimal;
import java.time.Instant;

public record BloodRequestResponse(
        Long id,
        BloodType bloodType,
        ProvinciaResponse provincia,
        LocalidadResponse localidad,
        String direccion,
        String nombreInstitucion,
        String calle,
        String numeracion,
        String direccionFormateada,
        BigDecimal latitud,
        BigDecimal longitud,
        String ubicacionExternalId,
        String description,
        BloodRequestStatus status,
        String creatorName,
        Instant createdAt
) {

    public static BloodRequestResponse from(BloodRequest bloodRequest) {
        return new BloodRequestResponse(
                bloodRequest.getId(),
                bloodRequest.getBloodType(),
                new ProvinciaResponse(
                        bloodRequest.getProvincia().getId(),
                        bloodRequest.getProvincia().getNombre()
                ),
                new LocalidadResponse(
                        bloodRequest.getLocalidad().getId(),
                        bloodRequest.getLocalidad().getNombre(),
                        bloodRequest.getProvincia().getId()
                ),
                bloodRequest.getDireccion(),
                bloodRequest.getNombreInstitucion(),
                bloodRequest.getCalle(),
                bloodRequest.getNumeracion(),
                bloodRequest.getDireccionFormateada(),
                bloodRequest.getLatitud(),
                bloodRequest.getLongitud(),
                bloodRequest.getUbicacionExternalId(),
                bloodRequest.getDescription(),
                bloodRequest.getStatus(),
                bloodRequest.getCreator().getName(),
                bloodRequest.getCreatedAt()
        );
    }

}
