package com.donate.app.controller;

import com.donate.app.dto.BloodRequestResponse;
import com.donate.app.dto.CreateBloodRequestRequest;
import com.donate.app.dto.DonorEnrollmentResponse;
import com.donate.app.dto.DonorEnrollmentSummaryResponse;
import com.donate.app.dto.UpdateBloodRequestRequest;
import com.donate.app.dto.UpdateBloodRequestStatusRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.BloodType;
import com.donate.app.service.BloodRequestService;
import com.donate.app.service.DonorEnrollmentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blood-requests")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;
    private final DonorEnrollmentService donorEnrollmentService;

    public BloodRequestController(
            BloodRequestService bloodRequestService,
            DonorEnrollmentService donorEnrollmentService
    ) {
        this.bloodRequestService = bloodRequestService;
        this.donorEnrollmentService = donorEnrollmentService;
    }

    @PostMapping
    public BloodRequestResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateBloodRequestRequest request
    ) {
        return bloodRequestService.create(jwt, request);
    }

    @GetMapping("/mine")
    public List<BloodRequestResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return bloodRequestService.listMine(jwt);
    }

    @GetMapping("/for-me")
    public List<BloodRequestResponse> listForMe(@AuthenticationPrincipal Jwt jwt) {
        return bloodRequestService.listForMe(jwt);
    }

    @GetMapping
    public List<BloodRequestResponse> list(
            @RequestParam(required = false) Long provinciaId,
            @RequestParam(required = false) List<Long> localidadIds,
            @RequestParam(required = false) BloodType bloodType,
            @RequestParam(required = false) BloodRequestStatus status
    ) {
        return bloodRequestService.list(provinciaId, localidadIds, bloodType, status);
    }

    @GetMapping("/{bloodRequestId}")
    public BloodRequestResponse getById(@PathVariable Long bloodRequestId) {
        return bloodRequestService.getById(bloodRequestId);
    }

    @PatchMapping("/{bloodRequestId}")
    public BloodRequestResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bloodRequestId,
            @RequestBody UpdateBloodRequestRequest request
    ) {
        return bloodRequestService.update(jwt, bloodRequestId, request);
    }

    @PatchMapping("/{bloodRequestId}/status")
    public BloodRequestResponse updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bloodRequestId,
            @RequestBody UpdateBloodRequestStatusRequest request
    ) {
        return bloodRequestService.updateStatus(jwt, bloodRequestId, request);
    }

    @GetMapping("/{bloodRequestId}/enrollments")
    public List<DonorEnrollmentSummaryResponse> listEnrollments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bloodRequestId
    ) {
        return donorEnrollmentService.listEnrollmentsForCase(jwt, bloodRequestId);
    }

    @PostMapping("/{bloodRequestId}/enrollments")
    public DonorEnrollmentResponse enroll(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bloodRequestId
    ) {
        return donorEnrollmentService.enroll(jwt, bloodRequestId);
    }

}
