package com.donate.app.service;

import com.donate.app.dto.DonorEnrollmentResponse;
import com.donate.app.dto.DonorEnrollmentSummaryResponse;
import com.donate.app.model.BloodRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.DonorEnrollment;
import com.donate.app.model.User;
import com.donate.app.repository.BloodRequestRepository;
import com.donate.app.repository.DonorEnrollmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DonorEnrollmentService {

    private final DonorEnrollmentRepository donorEnrollmentRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final UserService userService;

    public DonorEnrollmentService(
            DonorEnrollmentRepository donorEnrollmentRepository,
            BloodRequestRepository bloodRequestRepository,
            UserService userService
    ) {
        this.donorEnrollmentRepository = donorEnrollmentRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.userService = userService;
    }

    @Transactional
    public DonorEnrollmentResponse enroll(Jwt jwt, Long bloodRequestId) {
        // Take the lock before any consistent read can establish an InnoDB snapshot.
        User donor = userService.requireAuthenticatedUserForUpdate(jwt);
        if (!donor.isProfileComplete()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete your profile before enrolling");
        }

        BloodRequest bloodRequest = bloodRequestRepository.findByIdWithDetails(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));

        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blood request is not open for enrollment");
        }

        if (bloodRequest.getCreator().getId().equals(donor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot enroll in your own case");
        }

        if (!donor.getBloodTypesInteres().contains(bloodRequest.getBloodType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This case blood type is not in your interests"
            );
        }

        if (donorEnrollmentRepository.existsByBloodRequestIdAndDonorId(bloodRequestId, donor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already enrolled in this case");
        }

        if (donorEnrollmentRepository.existsActiveEnrollmentByDonorId(donor.getId(), BloodRequestStatus.OPEN)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already enrolled in another open case");
        }

        DonorEnrollment enrollment = new DonorEnrollment();
        enrollment.setBloodRequest(bloodRequest);
        enrollment.setDonor(donor);

        return DonorEnrollmentResponse.from(donorEnrollmentRepository.save(enrollment));
    }

    @Transactional
    public void cancelActiveEnrollment(Jwt jwt) {
        User donor = userService.requireAuthenticatedUser(jwt);

        DonorEnrollment enrollment = donorEnrollmentRepository
                .findActiveByDonorId(donor.getId(), BloodRequestStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active enrollment found"));

        donorEnrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public DonorEnrollmentResponse getActiveEnrollment(Jwt jwt) {
        User donor = userService.requireAuthenticatedUser(jwt);

        return donorEnrollmentRepository
                .findActiveByDonorId(donor.getId(), BloodRequestStatus.OPEN)
                .map(DonorEnrollmentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active enrollment found"));
    }

    @Transactional(readOnly = true)
    public List<DonorEnrollmentSummaryResponse> listEnrollmentsForCase(Jwt jwt, Long bloodRequestId) {
        BloodRequest bloodRequest = bloodRequestRepository.findByIdWithDetails(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));

        User user = userService.requireAuthenticatedUser(jwt);
        if (!bloodRequest.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view enrollments for your own cases");
        }

        return donorEnrollmentRepository.findByBloodRequestIdWithDonor(bloodRequestId).stream()
                .map(DonorEnrollmentSummaryResponse::from)
                .toList();
    }

}
