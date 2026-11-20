package com.donate.app.service;

import com.donate.app.dto.UpdateProfileRequest;
import com.donate.app.dto.UserResponse;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.model.User;
import com.donate.app.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LocationService locationService;
    private final Auth0UserInfoClient auth0UserInfoClient;

    public UserService(
            UserRepository userRepository,
            LocationService locationService,
            Auth0UserInfoClient auth0UserInfoClient
    ) {
        this.userRepository = userRepository;
        this.locationService = locationService;
        this.auth0UserInfoClient = auth0UserInfoClient;
    }

    @Transactional
    public User findOrCreateFromJwt(Jwt jwt) {
        return userRepository.findByAuth0Id(jwt.getSubject())
                .map(existing -> syncIdentity(existing, jwt))
                .orElseGet(() -> createFromJwt(jwt));
    }

    @Transactional(readOnly = true)
    public User requireAuthenticatedUser(Jwt jwt) {
        return userRepository.findByAuth0Id(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    User requireAuthenticatedUserForUpdate(Jwt jwt) {
        return userRepository.findByAuth0IdForUpdate(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Jwt jwt) {
        User user = requireAuthenticatedUser(jwt);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(Jwt jwt, UpdateProfileRequest request) {
        validateProfileRequest(request);

        User user = requireAuthenticatedUser(jwt);
        Provincia provincia = locationService.requireProvincia(request.provinciaId());
        List<Localidad> localidades = locationService.requireLocalidadesInProvincia(
                request.localidadIds(),
                request.provinciaId()
        );

        user.getBloodTypesInteres().clear();
        user.getBloodTypesInteres().addAll(new HashSet<>(request.bloodTypes()));
        user.setProvincia(provincia);
        user.getLocalidadesInteres().clear();
        user.getLocalidadesInteres().addAll(new HashSet<>(localidades));

        return UserResponse.from(userRepository.save(user));
    }

    private User createFromJwt(Jwt jwt) {
        ResolvedIdentity identity = resolveIdentity(jwt);
        User user = new User();
        user.setAuth0Id(jwt.getSubject());
        user.setEmail(identity.email());
        user.setName(identity.name());
        return userRepository.save(user);
    }

    private User syncIdentity(User user, Jwt jwt) {
        ResolvedIdentity identity = resolveIdentity(jwt);
        user.setEmail(identity.email());
        user.setName(identity.name());
        return userRepository.save(user);
    }

    private ResolvedIdentity resolveIdentity(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        if (email != null && !email.isBlank()) {
            return new ResolvedIdentity(email, blankToNull(name));
        }

        Map<String, Object> profile = auth0UserInfoClient.fetchProfile(jwt.getTokenValue());
        Object emailClaim = profile.get("email");
        if (!(emailClaim instanceof String resolvedEmail) || resolvedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to resolve email from Auth0 profile");
        }

        Object nameClaim = profile.get("name");
        String resolvedName = nameClaim instanceof String value ? blankToNull(value) : blankToNull(name);
        return new ResolvedIdentity(resolvedEmail, resolvedName);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record ResolvedIdentity(String email, String name) {}

    private void validateProfileRequest(UpdateProfileRequest request) {
        if (request.bloodTypes() == null || request.bloodTypes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one blood type is required");
        }
        if (request.bloodTypes().stream().anyMatch(type -> type == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bloodTypes cannot contain null values");
        }
        if (request.bloodTypes().size() != Set.copyOf(request.bloodTypes()).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bloodTypes cannot contain duplicates");
        }
        if (request.provinciaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "provinciaId is required");
        }
    }

}
