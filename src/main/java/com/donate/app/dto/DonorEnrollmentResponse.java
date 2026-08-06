package com.donate.app.dto;

import com.donate.app.model.DonorEnrollment;

import java.time.Instant;

public record DonorEnrollmentResponse(
        Long id,
        BloodRequestResponse bloodRequest,
        Instant createdAt
) {

    public static DonorEnrollmentResponse from(DonorEnrollment enrollment) {
        return new DonorEnrollmentResponse(
                enrollment.getId(),
                BloodRequestResponse.from(enrollment.getBloodRequest()),
                enrollment.getCreatedAt()
        );
    }

}
