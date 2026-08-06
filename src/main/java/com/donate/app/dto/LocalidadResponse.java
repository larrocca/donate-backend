package com.donate.app.dto;

import com.donate.app.model.Localidad;

public record LocalidadResponse(
        Long id,
        String nombre,
        Long provinciaId
) {

    public static LocalidadResponse from(Localidad localidad) {
        return new LocalidadResponse(
                localidad.getId(),
                localidad.getNombre(),
                localidad.getProvincia().getId()
        );
    }

}
