package com.donate.app.integration.nominatim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimPlaceResponse(
        @JsonProperty("place_id")
        Long placeId,
        @JsonProperty("display_name")
        String displayName,
        String lat,
        String lon,
        Map<String, String> address
) {
}
