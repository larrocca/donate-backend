package com.donate.app.dto;

import java.math.BigDecimal;

public record AddressCandidateResponse(
        String externalId,
        String institutionName,
        String formattedAddress,
        String province,
        String locality,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
