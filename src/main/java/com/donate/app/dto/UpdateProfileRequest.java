package com.donate.app.dto;

import com.donate.app.model.BloodType;

import java.util.List;

public record UpdateProfileRequest(
        List<BloodType> bloodTypes,
        Long provinciaId,
        List<Long> localidadIds
) {
}
