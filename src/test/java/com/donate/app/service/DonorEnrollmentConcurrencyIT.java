package com.donate.app.service;

import com.donate.app.dto.DonorEnrollmentResponse;
import com.donate.app.dto.UpdateProfileRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.BloodType;
import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.model.User;
import com.donate.app.repository.DonorEnrollmentRepository;
import com.donate.app.repository.LocalidadRepository;
import com.donate.app.repository.ProvinciaRepository;
import com.donate.app.repository.UserRepository;
import com.donate.app.support.TestBloodRequestFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:mysql:8.4:///enrollment_race",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.hikari.transaction-isolation=TRANSACTION_REPEATABLE_READ",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DonorEnrollmentConcurrencyIT {

    @Autowired
    private DonorEnrollmentService enrollmentService;
    @Autowired
    private DonorEnrollmentRepository enrollmentRepository;
    @Autowired
    private BloodRequestService bloodRequestService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProvinciaRepository provinciaRepository;
    @Autowired
    private LocalidadRepository localidadRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void withoutDonorLockBothTransactionsCommitActiveEnrollments() throws Exception {
        Scenario scenario = createScenario();
        CyclicBarrier afterActiveCheck = new CyclicBarrier(2);
        Set<Long> connections = ConcurrentHashMap.newKeySet();
        MethodInterceptor coordinateAfterQuery = invocation -> {
            Object result = invocation.proceed();
            if (invocation.getMethod().getName().equals("existsActiveEnrollmentByDonorId")
                    && Long.valueOf(scenario.donorId()).equals(invocation.getArguments()[0])
                    && invocation.getArguments()[1] == BloodRequestStatus.OPEN) {
                assertThat(result).as("active enrollment check before either insert").isEqualTo(false);
                recordTransaction(connections);
                afterActiveCheck.await(15, TimeUnit.SECONDS);
            }
            return result;
        };
        // Restore the historical non-locking read only for this donor. The service and MySQL stay real.
        MethodInterceptor historicalRead = invocation -> {
            if (isDonorLock(invocation, scenario)) {
                return userRepository.findByAuth0Id(scenario.donor().getSubject());
            }
            return invocation.proceed();
        };
        Advised userProxy = (Advised) userRepository;
        Advised enrollmentProxy = (Advised) enrollmentRepository;
        userProxy.addAdvice(0, historicalRead);
        enrollmentProxy.addAdvice(0, coordinateAfterQuery);
        List<DonorEnrollmentResponse> responses;
        try {
            responses = runConcurrently(
                    () -> enrollmentService.enroll(scenario.donor(), scenario.firstCaseId()),
                    () -> enrollmentService.enroll(scenario.donor(), scenario.secondCaseId()));
        } finally {
            enrollmentProxy.removeAdvice(coordinateAfterQuery);
            userProxy.removeAdvice(historicalRead);
        }

        assertThat(connections).hasSize(2);
        var first = responses.get(0);
        var second = responses.get(1);
        assertThat(first.id()).isNotNull().isNotEqualTo(second.id());
        assertThat(second.id()).isNotNull();
        assertThat(first.bloodRequest().id()).isEqualTo(scenario.firstCaseId());
        assertThat(second.bloodRequest().id()).isEqualTo(scenario.secondCaseId());
        assertThat(activeEnrollments(scenario.donorId())).containsExactlyInAnyOrder(
                new EnrollmentRow(first.id(), scenario.firstCaseId()),
                new EnrollmentRow(second.id(), scenario.secondCaseId()));
    }

    @Test
    void withDonorLockOneTransactionCommitsAndTheOtherReturnsConflict() throws Exception {
        Scenario scenario = createScenario();
        CyclicBarrier beforeLock = new CyclicBarrier(2);
        Set<Long> connections = ConcurrentHashMap.newKeySet();
        MethodInterceptor coordinateBeforeLock = invocation -> {
            if (!isDonorLock(invocation, scenario)) {
                return invocation.proceed();
            }
            // Meet before either transaction can acquire the lock. No table reads precede it.
            beforeLock.await(15, TimeUnit.SECONDS);
            Optional<?> lockedDonor = (Optional<?>) invocation.proceed();
            User donor = (User) lockedDonor.orElseThrow();
            assertThat(entityManager.getLockMode(donor)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
            recordTransaction(connections);
            return lockedDonor;
        };
        Advised userProxy = (Advised) userRepository;
        userProxy.addAdvice(0, coordinateBeforeLock);
        List<Attempt> attempts;
        try {
            attempts = runConcurrently(
                    () -> attempt(scenario.donor(), scenario.firstCaseId()),
                    () -> attempt(scenario.donor(), scenario.secondCaseId()));
        } finally {
            userProxy.removeAdvice(coordinateBeforeLock);
        }

        assertThat(connections).as("independent MySQL connections").hasSize(2);
        var successes = attempts.stream().filter(attempt -> attempt.response() != null).toList();
        var conflicts = attempts.stream().filter(attempt -> attempt.conflict() != null).toList();
        assertThat(successes).hasSize(1);
        assertThat(conflicts).hasSize(1);
        Attempt winner = successes.get(0);
        Attempt loser = conflicts.get(0);
        assertThat(loser.conflict().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(loser.conflict().getReason()).isEqualTo("You are already enrolled in another open case");
        assertThat(winner.response().id()).isNotNull();
        assertThat(winner.response().bloodRequest().id()).isEqualTo(winner.caseId());
        assertThat(activeEnrollments(scenario.donorId()))
                .containsExactly(new EnrollmentRow(winner.response().id(), winner.caseId()));
    }

    private Scenario createScenario() {
        String suffix = UUID.randomUUID().toString();
        Provincia provincia = new Provincia();
        provincia.setNombre("Test " + suffix);
        provincia = provinciaRepository.save(provincia);

        Localidad localidad = new Localidad();
        localidad.setNombre("La Plata");
        localidad.setProvincia(provincia);
        localidad = localidadRepository.save(localidad);

        Jwt creator = completeProfile("creator-" + suffix, provincia.getId(), localidad.getId());
        Jwt donor = completeProfile("donor-" + suffix, provincia.getId(), localidad.getId());
        long donorId = userService.requireAuthenticatedUser(donor).getId();
        var firstCase = bloodRequestService.create(creator, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE, provincia.getId(), localidad.getId(), "Primer caso"));
        var secondCase = bloodRequestService.create(creator, TestBloodRequestFactory.validCreateRequest(
                BloodType.A_POSITIVE, provincia.getId(), localidad.getId(), "Segundo caso"));
        assertThat(firstCase.id()).isNotEqualTo(secondCase.id());
        assertThat(List.of(firstCase.status(), secondCase.status())).containsOnly(BloodRequestStatus.OPEN);
        return new Scenario(donor, donorId, firstCase.id(), secondCase.id());
    }

    private boolean isDonorLock(org.aopalliance.intercept.MethodInvocation invocation, Scenario scenario) {
        return invocation.getMethod().getName().equals("findByAuth0IdForUpdate")
                && scenario.donor().getSubject().equals(invocation.getArguments()[0]);
    }

    private void recordTransaction(Set<Long> connections) {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        assertThat(jdbc.queryForObject("SELECT @@transaction_isolation", String.class))
                .isEqualTo("REPEATABLE-READ");
        connections.add(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
    }

    private Attempt attempt(Jwt donor, long caseId) {
        try {
            return new Attempt(caseId, enrollmentService.enroll(donor, caseId), null);
        } catch (ResponseStatusException exception) {
            return new Attempt(caseId, null, exception);
        }
    }

    private <T> List<T> runConcurrently(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            var firstAttempt = workers.submit(first);
            var secondAttempt = workers.submit(second);
            return List.of(firstAttempt.get(25, TimeUnit.SECONDS), secondAttempt.get(25, TimeUnit.SECONDS));
        } finally {
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS))
                    .as("enrollment workers terminated").isTrue();
        }
    }

    private List<EnrollmentRow> activeEnrollments(long donorId) {
        return jdbc.query("""
                SELECT de.id, de.blood_request_id
                FROM donor_enrollments de
                JOIN blood_requests br ON br.id = de.blood_request_id
                WHERE de.donor_id = ? AND br.status = ?
                """, (rs, row) -> new EnrollmentRow(rs.getLong("id"), rs.getLong("blood_request_id")),
                donorId, BloodRequestStatus.OPEN.name());
    }

    private Jwt completeProfile(String subject, Long provinciaId, Long localidadId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .claim("email", subject + "@example.test")
                .claim("name", subject)
                .build();
        userService.findOrCreateFromJwt(jwt);
        userService.updateProfile(jwt, new UpdateProfileRequest(
                List.of(BloodType.A_POSITIVE), provinciaId, List.of(localidadId)));
        return jwt;
    }

    private record EnrollmentRow(long id, long bloodRequestId) {
    }

    private record Scenario(Jwt donor, long donorId, long firstCaseId, long secondCaseId) {
    }

    private record Attempt(long caseId, DonorEnrollmentResponse response, ResponseStatusException conflict) {
    }
}
