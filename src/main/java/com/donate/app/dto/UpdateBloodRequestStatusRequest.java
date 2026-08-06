package com.donate.app.dto;

import com.donate.app.model.BloodRequestStatus;

public record UpdateBloodRequestStatusRequest(
        BloodRequestStatus status
) {
}
