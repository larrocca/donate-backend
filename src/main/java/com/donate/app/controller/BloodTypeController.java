package com.donate.app.controller;

import com.donate.app.dto.BloodTypeResponse;
import com.donate.app.model.BloodType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/blood-types")
public class BloodTypeController {

    @GetMapping
    public List<BloodTypeResponse> list() {
        return Arrays.stream(BloodType.values())
                .map(BloodTypeResponse::from)
                .toList();
    }

}
