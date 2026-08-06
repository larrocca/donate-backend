package com.donate.app.service;

import com.donate.app.model.BloodType;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.model.User;
import com.donate.app.repository.LocalidadRepository;
import com.donate.app.repository.ProvinciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private LocalidadRepository localidadRepository;

    private Provincia provincia;
    private Localidad localidad;

    @BeforeEach
    void setUp() {
        provincia = new Provincia();
        provincia.setNombre("Córdoba");
        provincia = provinciaRepository.save(provincia);

        localidad = new Localidad();
        localidad.setNombre("Córdoba Capital");
        localidad.setProvincia(provincia);
        localidad = localidadRepository.save(localidad);
    }

    @Test
    void findOrCreateFromJwtCreatesUserOnFirstLogin() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("auth0|123")
                .claim("email", "donor@example.com")
                .claim("name", "Jane Donor")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        User user = userService.findOrCreateFromJwt(jwt);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getAuth0Id()).isEqualTo("auth0|123");
        assertThat(user.getEmail()).isEqualTo("donor@example.com");
        assertThat(user.getName()).isEqualTo("Jane Donor");
        assertThat(user.isProfileComplete()).isFalse();
    }

    @Test
    void updateProfileCompletesUserProfile() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("auth0|456")
                .claim("email", "donor2@example.com")
                .claim("name", "John Donor")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        userService.findOrCreateFromJwt(jwt);

        var response = userService.updateProfile(jwt, new com.donate.app.dto.UpdateProfileRequest(
                List.of(BloodType.O_POSITIVE, BloodType.O_NEGATIVE),
                provincia.getId(),
                List.of(localidad.getId())
        ));

        assertThat(response.profileComplete()).isTrue();
        assertThat(response.bloodTypesInteres()).containsExactlyInAnyOrder(BloodType.O_POSITIVE, BloodType.O_NEGATIVE);
        assertThat(response.provincia().id()).isEqualTo(provincia.getId());
        assertThat(response.localidadesInteres()).hasSize(1);
        assertThat(response.localidadesInteres().get(0).id()).isEqualTo(localidad.getId());
    }

}
