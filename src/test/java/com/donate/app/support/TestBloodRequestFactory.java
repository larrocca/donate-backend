package com.donate.app.support;

import com.donate.app.dto.CreateBloodRequestRequest;
import com.donate.app.model.BloodType;

import java.math.BigDecimal;

public final class TestBloodRequestFactory {

    private TestBloodRequestFactory() {
    }

    public static CreateBloodRequestRequest validCreateRequest(
            BloodType bloodType,
            Long provinciaId,
            Long localidadId
    ) {
        return validCreateRequest(bloodType, provinciaId, localidadId, "Urgente");
    }

    public static CreateBloodRequestRequest validCreateRequest(
            BloodType bloodType,
            Long provinciaId,
            Long localidadId,
            String description
    ) {
        return new CreateBloodRequestRequest(
                bloodType,
                provinciaId,
                localidadId,
                "Hospital Italiano",
                "Calle 123",
                "100",
                description,
                "Hospital Italiano, Calle 123 100, La Plata, Buenos Aires, Argentina",
                new BigDecimal("-34.9205000"),
                new BigDecimal("-57.9536000"),
                "123456789"
        );
    }
}
