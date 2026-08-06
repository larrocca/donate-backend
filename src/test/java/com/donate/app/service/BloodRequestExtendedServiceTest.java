package com.donate.app.service;

import com.donate.app.dto.CreateBloodRequestRequest;
import com.donate.app.support.TestBloodRequestFactory;
import com.donate.app.dto.UpdateBloodRequestRequest;
import com.donate.app.dto.UpdateBloodRequestStatusRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.BloodType;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BloodRequestExtendedServiceTest {

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.donate.app.repository.ProvinciaRepository provinciaRepository;

    @Autowired
    private com.donate.app.repository.LocalidadRepository localidadRepository;

    private Provincia provincia;
    private Localidad localidadLaPlata;
    private Localidad localidadMarDelPlata;
    private Jwt creatorJwt;
    private Jwt donorJwt;

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

        creatorJwt = jwtFor("auth0|ext-creator", "creator@example.com", "Creator");
        donorJwt = jwtFor("auth0|ext-donor", "donor@example.com", "Donor");

        completeProfile(creatorJwt, List.of(BloodType.A_POSITIVE), List.of(localidadLaPlata.getId()));
        completeProfile(donorJwt, List.of(BloodType.A_POSITIVE, BloodType.O_POSITIVE), List.of(localidadLaPlata.getId()));
    }

    @Test
    void listMineReturnsOnlyCreatorCases() {
        bloodRequestService.create(creatorJwt, caseRequest(localidadLaPlata.getId(), BloodType.A_POSITIVE, "Hospital 1"));

        Jwt otherCreator = jwtFor("auth0|ext-creator2", "creator2@example.com", "Creator 2");
        completeProfile(otherCreator, List.of(BloodType.O_NEGATIVE), List.of(localidadLaPlata.getId()));
        bloodRequestService.create(otherCreator, caseRequest(localidadLaPlata.getId(), BloodType.O_NEGATIVE, "Hospital 2"));

        var mine = bloodRequestService.listMine(creatorJwt);

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).description()).isEqualTo("Hospital 1");
    }

    @Test
    void listForMeMatchesInterestsAndExcludesOwnCases() {
        bloodRequestService.create(creatorJwt, caseRequest(localidadLaPlata.getId(), BloodType.A_POSITIVE, "Match"));

        Jwt otherCreator = jwtFor("auth0|ext-creator3", "creator3@example.com", "Creator 3");
        completeProfile(otherCreator, List.of(BloodType.B_NEGATIVE), List.of(localidadLaPlata.getId()));
        bloodRequestService.create(otherCreator, caseRequest(localidadLaPlata.getId(), BloodType.B_NEGATIVE, "No match type"));

        bloodRequestService.create(otherCreator, caseRequest(localidadMarDelPlata.getId(), BloodType.A_POSITIVE, "No match location"));

        var forMe = bloodRequestService.listForMe(donorJwt);

        assertThat(forMe).hasSize(1);
        assertThat(forMe.get(0).description()).isEqualTo("Match");
    }

    @Test
    void updateAndCloseCase() {
        Long caseId = bloodRequestService.create(
                creatorJwt,
                caseRequest(localidadLaPlata.getId(), BloodType.A_POSITIVE, "Original")
        ).id();

        var updated = bloodRequestService.update(creatorJwt, caseId, new UpdateBloodRequestRequest(
                "Nueva dirección",
                "Actualizado"
        ));

        assertThat(updated.direccion()).isEqualTo("Nueva dirección");
        assertThat(updated.description()).isEqualTo("Actualizado");

        var closed = bloodRequestService.updateStatus(creatorJwt, caseId, new UpdateBloodRequestStatusRequest(
                BloodRequestStatus.FULFILLED
        ));

        assertThat(closed.status()).isEqualTo(BloodRequestStatus.FULFILLED);

        assertThatThrownBy(() -> bloodRequestService.update(creatorJwt, caseId, new UpdateBloodRequestRequest(
                "Otra",
                null
        ))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void onlyCreatorCanUpdateCase() {
        Long caseId = bloodRequestService.create(
                creatorJwt,
                caseRequest(localidadLaPlata.getId(), BloodType.A_POSITIVE, "Privado")
        ).id();

        assertThatThrownBy(() -> bloodRequestService.updateStatus(
                donorJwt,
                caseId,
                new UpdateBloodRequestStatusRequest(BloodRequestStatus.CANCELLED)
        )).isInstanceOf(ResponseStatusException.class);
    }

    private CreateBloodRequestRequest caseRequest(Long localidadId, BloodType bloodType, String description) {
        return TestBloodRequestFactory.validCreateRequest(
                bloodType,
                provincia.getId(),
                localidadId,
                description
        );
    }

    private Jwt jwtFor(String subject, String email, String name) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("email", email)
                .claim("name", name)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private void completeProfile(Jwt jwt, List<BloodType> bloodTypes, List<Long> localidadIds) {
        userService.findOrCreateFromJwt(jwt);
        userService.updateProfile(jwt, new com.donate.app.dto.UpdateProfileRequest(
                bloodTypes,
                provincia.getId(),
                localidadIds
        ));
    }

}
