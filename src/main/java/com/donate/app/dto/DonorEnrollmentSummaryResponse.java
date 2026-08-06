package com.donate.app.dto;

import com.donate.app.model.DonorEnrollment;

import java.time.Instant;

public record DonorEnrollmentSummaryResponse(
        Long id,
        String donorName,
        String donorEmail,
        Instant createdAt
) {

    public static DonorEnrollmentSummaryResponse from(DonorEnrollment enrollment) {
        return new DonorEnrollmentSummaryResponse(
                enrollment.getId(),
                enrollment.getDonor().getName(),
                enrollment.getDonor().getEmail(),
                enrollment.getCreatedAt()
        );
    }

}
