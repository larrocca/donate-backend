package com.donate.app.dto;

import com.donate.app.model.BloodRequestStatus;

public record UpdateBloodRequestRequest(
        String direccion,
        String description
) {
}
