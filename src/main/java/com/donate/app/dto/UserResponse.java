package com.donate.app.dto;

import com.donate.app.model.BloodType;
import com.donate.app.model.User;

import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String name,
        List<BloodType> bloodTypesInteres,
        ProvinciaResponse provincia,
        List<LocalidadResponse> localidadesInteres,
        boolean profileComplete
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getBloodTypesInteres().stream().sorted().toList(),
                user.getProvincia() == null ? null : new ProvinciaResponse(
                        user.getProvincia().getId(),
                        user.getProvincia().getNombre()
                ),
                user.getLocalidadesInteres().stream()
                        .map(localidad -> new LocalidadResponse(
                                localidad.getId(),
                                localidad.getNombre(),
                                user.getProvincia().getId()
                        ))
                        .toList(),
                user.isProfileComplete()
        );
    }

}
