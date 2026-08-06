package com.donate.app.service;

import com.donate.app.dto.LocalidadResponse;
import com.donate.app.dto.ProvinciaResponse;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.repository.LocalidadRepository;
import com.donate.app.repository.ProvinciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LocationService {

    private final ProvinciaRepository provinciaRepository;
    private final LocalidadRepository localidadRepository;

    public LocationService(ProvinciaRepository provinciaRepository, LocalidadRepository localidadRepository) {
        this.provinciaRepository = provinciaRepository;
        this.localidadRepository = localidadRepository;
    }

    @Transactional(readOnly = true)
    public List<ProvinciaResponse> listProvincias() {
        return provinciaRepository.findAll().stream()
                .map(provincia -> new ProvinciaResponse(provincia.getId(), provincia.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocalidadResponse> listLocalidadesByProvincia(Long provinciaId) {
        requireProvincia(provinciaId);
        return localidadRepository.findByProvinciaIdOrderByNombreAsc(provinciaId).stream()
                .map(localidad -> new LocalidadResponse(localidad.getId(), localidad.getNombre(), provinciaId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Provincia requireProvincia(Long provinciaId) {
        return provinciaRepository.findById(provinciaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provincia not found"));
    }

    @Transactional(readOnly = true)
    public Localidad requireLocalidadInProvincia(Long localidadId, Long provinciaId) {
        return localidadRepository.findById(localidadId)
                .filter(localidad -> localidad.getProvincia().getId().equals(provinciaId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Localidad does not belong to the selected provincia"
                ));
    }

    @Transactional(readOnly = true)
    public List<Localidad> requireLocalidadesInProvincia(List<Long> localidadIds, Long provinciaId) {
        if (localidadIds == null || localidadIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one localidad is required");
        }

        List<Localidad> localidades = localidadRepository.findByIdInAndProvinciaId(localidadIds, provinciaId);
        if (localidades.size() != localidadIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more localidades are invalid for the selected provincia"
            );
        }

        return localidades;
    }

}
