package com.donate.app.service;

import com.donate.app.dto.CreateBloodRequestRequest;
import com.donate.app.model.BloodType;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.repository.LocalidadRepository;
import com.donate.app.repository.ProvinciaRepository;
import com.donate.app.support.TestBloodRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BloodRequestServiceTest {

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private LocalidadRepository localidadRepository;

    private Provincia provincia;
    private Localidad localidadLaPlata;
    private Localidad localidadMarDelPlata;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        provincia = new Provincia();
        provincia.setNombre("Buenos Aires");
        provincia = provinciaRepository.save(provincia);

        localidadLaPlata = new Localidad();
        localidadLaPlata.setNombre("La Plata");
        localidadLaPlata.setProvincia(provincia);
        localidadLaPlata = localidadRepository.save(localidadLaPlata);

        localidadMarDelPlata = new Localidad();
        localidadMarDelPlata.setNombre("Mar del Plata");
        localidadMarDelPlata.setProvincia(provincia);
        localidadMarDelPlata = localidadRepository.save(localidadMarDelPlata);

        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("auth0|creator")
                .claim("email", "creator@example.com")
                .claim("name", "Case Creator")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        userService.findOrCreateFromJwt(jwt);
        userService.updateProfile(jwt, new com.donate.app.dto.UpdateProfileRequest(
                List.of(BloodType.A_POSITIVE),
                provincia.getId(),
                List.of(localidadLaPlata.getId())
        ));
    }

    @Test
    void createBloodRequest() {
        var response = bloodRequestService.create(jwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE,
                provincia.getId(),
                localidadLaPlata.getId(),
                "Urgente"
        ));

        assertThat(response.id()).isNotNull();
        assertThat(response.provincia().id()).isEqualTo(provincia.getId());
        assertThat(response.localidad().id()).isEqualTo(localidadLaPlata.getId());
        assertThat(response.direccionFormateada()).contains("Hospital Italiano");
        assertThat(response.latitud()).isNotNull();
        assertThat(response.longitud()).isNotNull();
        assertThat(response.creatorName()).isEqualTo("Case Creator");
    }

    @Test
    void createRequiresCompleteProfile() {
        Jwt incompleteJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("auth0|incomplete")
                .claim("email", "incomplete@example.com")
                .claim("name", "Incomplete User")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        userService.findOrCreateFromJwt(incompleteJwt);

        assertThatThrownBy(() -> bloodRequestService.create(incompleteJwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.O_POSITIVE,
                provincia.getId(),
                localidadLaPlata.getId()
        ))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listFiltersByProvinciaAndLocalidad() {
        bloodRequestService.create(jwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE,
                provincia.getId(),
                localidadLaPlata.getId(),
                "Hospital La Plata"
        ));
        bloodRequestService.create(jwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.B_NEGATIVE,
                provincia.getId(),
                localidadMarDelPlata.getId(),
                "Hospital Mar del Plata"
        ));

        var laPlataResults = bloodRequestService.list(
                provincia.getId(),
                List.of(localidadLaPlata.getId()),
                null,
                null
        );

        assertThat(laPlataResults).hasSize(1);
        assertThat(laPlataResults.get(0).localidad().id()).isEqualTo(localidadLaPlata.getId());

        var allInProvincia = bloodRequestService.list(provincia.getId(), null, null, null);

        assertThat(allInProvincia).hasSize(2);
    }

    @Test
    void listFiltersByBloodType() {
        bloodRequestService.create(jwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE,
                provincia.getId(),
                localidadLaPlata.getId(),
                "Hospital La Plata"
        ));
        bloodRequestService.create(jwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.O_NEGATIVE,
                provincia.getId(),
                localidadLaPlata.getId(),
                "Otra clínica"
        ));

        var results = bloodRequestService.list(null, null, BloodType.O_NEGATIVE, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bloodType()).isEqualTo(BloodType.O_NEGATIVE);
    }

}
