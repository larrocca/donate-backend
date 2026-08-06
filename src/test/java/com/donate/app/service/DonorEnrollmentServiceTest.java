package com.donate.app.service;

import com.donate.app.support.TestBloodRequestFactory;
import com.donate.app.model.BloodRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.BloodType;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.repository.BloodRequestRepository;
import com.donate.app.repository.LocalidadRepository;
import com.donate.app.repository.ProvinciaRepository;
import com.donate.app.repository.DonorEnrollmentRepository;
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
class DonorEnrollmentServiceTest {

    @Autowired
    private DonorEnrollmentService donorEnrollmentService;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private UserService userService;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorEnrollmentRepository donorEnrollmentRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private LocalidadRepository localidadRepository;

    private Provincia provincia;
    private Localidad localidad;
    private Jwt creatorJwt;
    private Jwt donorJwt;
    private Long bloodRequestId;

    @BeforeEach
    void setUp() {
        donorEnrollmentRepository.deleteAll();
        bloodRequestRepository.deleteAll();

        provincia = new Provincia();
        provincia.setNombre("Buenos Aires");
        provincia = provinciaRepository.save(provincia);

        localidad = new Localidad();
        localidad.setNombre("La Plata");
        localidad.setProvincia(provincia);
        localidad = localidadRepository.save(localidad);

        creatorJwt = jwtFor("auth0|enroll-creator", "creator@example.com", "Creator");
        donorJwt = jwtFor("auth0|enroll-donor", "donor@example.com", "Donor");

        completeProfile(creatorJwt);
        completeProfile(donorJwt);

        bloodRequestId = bloodRequestService.create(creatorJwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE,
                provincia.getId(),
                localidad.getId()
        )).id();
    }

    @Test
    void enrollInOpenCase() {
        var response = donorEnrollmentService.enroll(donorJwt, bloodRequestId);

        assertThat(response.id()).isNotNull();
        assertThat(response.bloodRequest().id()).isEqualTo(bloodRequestId);
        assertThat(donorEnrollmentService.getActiveEnrollment(donorJwt).id()).isEqualTo(response.id());
    }

    @Test
    void cannotEnrollInTwoOpenCases() {
        donorEnrollmentService.enroll(donorJwt, bloodRequestId);

        Jwt anotherDonorJwt = jwtFor("auth0|enroll-creator2", "creator2@example.com", "Creator 2");
        completeProfile(anotherDonorJwt);

        Long anotherCaseId = bloodRequestService.create(anotherDonorJwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE,
                provincia.getId(),
                localidad.getId(),
                "Otra clínica"
        )).id();

        assertThatThrownBy(() -> donorEnrollmentService.enroll(donorJwt, anotherCaseId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("another open case");
    }

    @Test
    void cannotEnrollInOwnCase() {
        assertThatThrownBy(() -> donorEnrollmentService.enroll(creatorJwt, bloodRequestId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("your own case");
    }

    @Test
    void cannotEnrollInClosedCase() {
        BloodRequest bloodRequest = bloodRequestRepository.findById(bloodRequestId).orElseThrow();
        bloodRequest.setStatus(BloodRequestStatus.FULFILLED);
        bloodRequestRepository.save(bloodRequest);

        assertThatThrownBy(() -> donorEnrollmentService.enroll(donorJwt, bloodRequestId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void canEnrollAfterPreviousCaseIsClosed() {
        donorEnrollmentService.enroll(donorJwt, bloodRequestId);

        BloodRequest bloodRequest = bloodRequestRepository.findById(bloodRequestId).orElseThrow();
        bloodRequest.setStatus(BloodRequestStatus.FULFILLED);
        bloodRequestRepository.save(bloodRequest);

        Jwt anotherCreatorJwt = jwtFor("auth0|enroll-creator3", "creator3@example.com", "Creator 3");
        completeProfile(anotherCreatorJwt);

        Long newCaseId = bloodRequestService.create(anotherCreatorJwt, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE,
                provincia.getId(),
                localidad.getId(),
                "Nuevo hospital"
        )).id();

        var response = donorEnrollmentService.enroll(donorJwt, newCaseId);

        assertThat(response.bloodRequest().id()).isEqualTo(newCaseId);
    }

    @Test
    void cannotEnrollWhenBloodTypeNotInInterests() {
        Jwt selectiveDonor = jwtFor("auth0|selective-donor", "selective@example.com", "Selective Donor");
        completeProfile(selectiveDonor, List.of(BloodType.O_POSITIVE), List.of(localidad.getId()));

        assertThatThrownBy(() -> donorEnrollmentService.enroll(selectiveDonor, bloodRequestId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not in your interests");
    }

    @Test
    void cancelActiveEnrollment() {
        donorEnrollmentService.enroll(donorJwt, bloodRequestId);
        donorEnrollmentService.cancelActiveEnrollment(donorJwt);

        assertThatThrownBy(() -> donorEnrollmentService.getActiveEnrollment(donorJwt))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void creatorCanListEnrollments() {
        donorEnrollmentService.enroll(donorJwt, bloodRequestId);

        var enrollments = donorEnrollmentService.listEnrollmentsForCase(creatorJwt, bloodRequestId);

        assertThat(enrollments).hasSize(1);
        assertThat(enrollments.get(0).donorEmail()).isEqualTo("donor@example.com");
    }

    @Test
    void nonCreatorCannotListEnrollments() {
        assertThatThrownBy(() -> donorEnrollmentService.listEnrollmentsForCase(donorJwt, bloodRequestId))
                .isInstanceOf(ResponseStatusException.class);
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

    private void completeProfile(Jwt jwt) {
        userService.findOrCreateFromJwt(jwt);
        userService.updateProfile(jwt, new com.donate.app.dto.UpdateProfileRequest(
                List.of(BloodType.A_POSITIVE),
                provincia.getId(),
                List.of(localidad.getId())
        ));
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
