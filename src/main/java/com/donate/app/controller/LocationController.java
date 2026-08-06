package com.donate.app.controller;

import com.donate.app.dto.LocalidadResponse;
import com.donate.app.dto.ProvinciaResponse;
import com.donate.app.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/provincias")
    public List<ProvinciaResponse> listProvincias() {
        return locationService.listProvincias();
    }

    @GetMapping("/provincias/{provinciaId}/localidades")
    public List<LocalidadResponse> listLocalidades(@PathVariable Long provinciaId) {
        return locationService.listLocalidadesByProvincia(provinciaId);
    }

}
