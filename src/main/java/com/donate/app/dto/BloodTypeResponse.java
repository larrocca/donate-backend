package com.donate.app.dto;

import com.donate.app.model.BloodType;

public record BloodTypeResponse(
        String code,
        String label
) {

    public static BloodTypeResponse from(BloodType bloodType) {
        return new BloodTypeResponse(bloodType.name(), formatLabel(bloodType));
    }

    private static String formatLabel(BloodType bloodType) {
        return switch (bloodType) {
            case A_POSITIVE -> "A+";
            case A_NEGATIVE -> "A-";
            case B_POSITIVE -> "B+";
            case B_NEGATIVE -> "B-";
            case AB_POSITIVE -> "AB+";
            case AB_NEGATIVE -> "AB-";
            case O_POSITIVE -> "O+";
            case O_NEGATIVE -> "O-";
        };
    }

}
