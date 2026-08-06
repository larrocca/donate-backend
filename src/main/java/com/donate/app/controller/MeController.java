package com.donate.app.controller;

import com.donate.app.dto.DonorEnrollmentResponse;
import com.donate.app.dto.UpdateProfileRequest;
import com.donate.app.dto.UserResponse;
import com.donate.app.service.DonorEnrollmentService;
import com.donate.app.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final DonorEnrollmentService donorEnrollmentService;

    public MeController(UserService userService, DonorEnrollmentService donorEnrollmentService) {
        this.userService = userService;
        this.donorEnrollmentService = donorEnrollmentService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(jwt);
    }

    @PatchMapping("/profile")
    public UserResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(jwt, request);
    }

    @GetMapping("/enrollment")
    public DonorEnrollmentResponse activeEnrollment(@AuthenticationPrincipal Jwt jwt) {
        return donorEnrollmentService.getActiveEnrollment(jwt);
    }

    @DeleteMapping("/enrollment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelEnrollment(@AuthenticationPrincipal Jwt jwt) {
        donorEnrollmentService.cancelActiveEnrollment(jwt);
    }

}
