package com.donate.app.service;

import com.donate.app.dto.BloodRequestResponse;
import com.donate.app.dto.CreateBloodRequestRequest;
import com.donate.app.dto.UpdateBloodRequestRequest;
import com.donate.app.dto.UpdateBloodRequestStatusRequest;
import com.donate.app.model.BloodRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.BloodType;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.model.User;
import com.donate.app.repository.BloodRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final UserService userService;
    private final LocationService locationService;

    public BloodRequestService(
            BloodRequestRepository bloodRequestRepository,
            UserService userService,
            LocationService locationService
    ) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.userService = userService;
        this.locationService = locationService;
    }

    @Transactional
    public BloodRequestResponse create(Jwt jwt, CreateBloodRequestRequest request) {
        validateCreateRequest(request);

        User creator = userService.requireAuthenticatedUser(jwt);
        if (!creator.isProfileComplete()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete your profile before creating a case");
        }

        Provincia provincia = locationService.requireProvincia(request.provinciaId());
        Localidad localidad = locationService.requireLocalidadInProvincia(request.localidadId(), request.provinciaId());

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setCreator(creator);
        bloodRequest.setBloodType(request.bloodType());
        bloodRequest.setProvincia(provincia);
        bloodRequest.setLocalidad(localidad);
        bloodRequest.setNombreInstitucion(trimToNull(request.nombreInstitucion()));
        bloodRequest.setCalle(request.calle().trim());
        bloodRequest.setNumeracion(request.numeracion().trim());
        bloodRequest.setDireccionFormateada(request.direccionFormateada().trim());
        bloodRequest.setDireccion(request.direccionFormateada().trim());
        bloodRequest.setLatitud(request.latitud());
        bloodRequest.setLongitud(request.longitud());
        bloodRequest.setUbicacionExternalId(request.ubicacionExternalId().trim());
        bloodRequest.setDescription(request.description());

        return BloodRequestResponse.from(bloodRequestRepository.save(bloodRequest));
    }

    @Transactional(readOnly = true)
    public BloodRequestResponse getById(Long bloodRequestId) {
        return BloodRequestResponse.from(requireBloodRequest(bloodRequestId));
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listMine(Jwt jwt) {
        User user = userService.requireAuthenticatedUser(jwt);
        return bloodRequestRepository.findByCreatorIdWithDetails(user.getId()).stream()
                .map(BloodRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listForMe(Jwt jwt) {
        User user = userService.requireAuthenticatedUser(jwt);
        if (!user.isProfileComplete()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete your profile to view recommended cases");
        }

        Set<Long> localidadIds = user.getLocalidadesInteres().stream()
                .map(Localidad::getId)
                .collect(Collectors.toSet());

        return bloodRequestRepository.findForUserInterests(
                BloodRequestStatus.OPEN,
                user.getBloodTypesInteres(),
                localidadIds,
                user.getId()
        ).stream()
                .map(BloodRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> list(
            Long provinciaId,
            List<Long> localidadIds,
            BloodType bloodType,
            BloodRequestStatus status
    ) {
        if (provinciaId != null) {
            locationService.requireProvincia(provinciaId);
        }

        List<Long> filteredLocalidadIds = normalizeLocalidadIds(localidadIds);
        if (filteredLocalidadIds != null && provinciaId != null) {
            locationService.requireLocalidadesInProvincia(filteredLocalidadIds, provinciaId);
        }

        BloodRequestStatus effectiveStatus = status != null ? status : BloodRequestStatus.OPEN;

        return bloodRequestRepository.search(effectiveStatus, provinciaId, filteredLocalidadIds, bloodType).stream()
                .map(BloodRequestResponse::from)
                .toList();
    }

    @Transactional
    public BloodRequestResponse update(Jwt jwt, Long bloodRequestId, UpdateBloodRequestRequest request) {
        BloodRequest bloodRequest = requireOwnedOpenBloodRequest(jwt, bloodRequestId);
        validateUpdateRequest(request);

        bloodRequest.setDireccion(request.direccion().trim());
        bloodRequest.setDescription(request.description());

        return BloodRequestResponse.from(bloodRequestRepository.save(bloodRequest));
    }

    @Transactional
    public BloodRequestResponse updateStatus(Jwt jwt, Long bloodRequestId, UpdateBloodRequestStatusRequest request) {
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (request.status() == BloodRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reopen a case through this endpoint");
        }

        BloodRequest bloodRequest = requireOwnedBloodRequest(jwt, bloodRequestId);

        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only open cases can change status");
        }

        bloodRequest.setStatus(request.status());
        return BloodRequestResponse.from(bloodRequestRepository.save(bloodRequest));
    }

    BloodRequest requireBloodRequest(Long bloodRequestId) {
        return bloodRequestRepository.findByIdWithDetails(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));
    }

    BloodRequest requireOwnedBloodRequest(Jwt jwt, Long bloodRequestId) {
        BloodRequest bloodRequest = requireBloodRequest(bloodRequestId);
        User user = userService.requireAuthenticatedUser(jwt);

        if (!bloodRequest.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own cases");
        }

        return bloodRequest;
    }

    private BloodRequest requireOwnedOpenBloodRequest(Jwt jwt, Long bloodRequestId) {
        BloodRequest bloodRequest = requireOwnedBloodRequest(jwt, bloodRequestId);

        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only open cases can be updated");
        }

        return bloodRequest;
    }

    private List<Long> normalizeLocalidadIds(List<Long> localidadIds) {
        if (localidadIds == null || localidadIds.isEmpty()) {
            return null;
        }
        return localidadIds;
    }

    private void validateCreateRequest(CreateBloodRequestRequest request) {
        if (request.bloodType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bloodType is required");
        }
        if (request.provinciaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "provinciaId is required");
        }
        if (request.localidadId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "localidadId is required");
        }
        if (request.calle() == null || request.calle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "calle is required");
        }
        if (request.numeracion() == null || request.numeracion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "numeracion is required");
        }
        if (request.direccionFormateada() == null || request.direccionFormateada().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmed location is required");
        }
        if (request.latitud() == null || request.longitud() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location coordinates are required");
        }
        if (request.ubicacionExternalId() == null || request.ubicacionExternalId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location externalId is required");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateUpdateRequest(UpdateBloodRequestRequest request) {
        if (request.direccion() == null || request.direccion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direccion is required");
        }
    }

}
