package com.donate.app.controller;

import com.donate.app.dto.AddressCandidateResponse;
import com.donate.app.dto.AddressSearchRequest;
import com.donate.app.service.LocationSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationSearchController {

    private final LocationSearchService locationSearchService;

    public LocationSearchController(LocationSearchService locationSearchService) {
        this.locationSearchService = locationSearchService;
    }

    @PostMapping("/search")
    public List<AddressCandidateResponse> search(@Valid @RequestBody AddressSearchRequest request) {
        return locationSearchService.search(request);
    }
}
