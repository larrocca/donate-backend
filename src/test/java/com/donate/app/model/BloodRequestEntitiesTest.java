package com.donate.app.model;

import com.donate.app.repository.BloodRequestRepository;
import com.donate.app.repository.DonorEnrollmentRepository;
import com.donate.app.repository.LocalidadRepository;
import com.donate.app.repository.ProvinciaRepository;
import com.donate.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BloodRequestEntitiesTest {

    @Autowired
    private UserRepository userRepository;

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

    @BeforeEach
    void setUp() {
        provincia = new Provincia();
        provincia.setNombre("Buenos Aires");
        provincia = provinciaRepository.save(provincia);

        localidad = new Localidad();
        localidad.setNombre("La Plata");
        localidad.setProvincia(provincia);
        localidad = localidadRepository.save(localidad);
    }

    @Test
    void persistBloodRequestAndDonorEnrollment() {
        User creator = new User();
        creator.setAuth0Id("auth0|creator");
        creator.setEmail("creator@example.com");
        creator.setName("Creator");
        creator.setBloodTypesInteres(new java.util.HashSet<>(List.of(BloodType.A_POSITIVE)));
        creator.setProvincia(provincia);
        creator.getLocalidadesInteres().add(localidad);
        creator = userRepository.save(creator);

        User donor = new User();
        donor.setAuth0Id("auth0|donor");
        donor.setEmail("donor@example.com");
        donor.setName("Donor");
        donor.setBloodTypesInteres(new java.util.HashSet<>(List.of(BloodType.A_POSITIVE)));
        donor.setProvincia(provincia);
        donor.getLocalidadesInteres().add(localidad);
        donor = userRepository.save(donor);

        BloodRequest request = new BloodRequest();
        request.setCreator(creator);
        request.setBloodType(BloodType.A_POSITIVE);
        request.setProvincia(provincia);
        request.setLocalidad(localidad);
        request.setDireccion("Hospital Italiano, calle 123");
        request.setDescription("Urgente para cirugía");
        request = bloodRequestRepository.save(request);

        DonorEnrollment enrollment = new DonorEnrollment();
        enrollment.setBloodRequest(request);
        enrollment.setDonor(donor);
        enrollment = donorEnrollmentRepository.save(enrollment);

        assertThat(request.getId()).isNotNull();
        assertThat(request.getStatus()).isEqualTo(BloodRequestStatus.OPEN);
        assertThat(request.getDireccion()).isEqualTo("Hospital Italiano, calle 123");
        assertThat(enrollment.getId()).isNotNull();
        assertThat(donorEnrollmentRepository.existsByBloodRequestIdAndDonorId(request.getId(), donor.getId())).isTrue();
    }

}
