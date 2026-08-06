package com.donate.app.service;

import com.donate.app.config.CacheConfig;
import com.donate.app.dto.AddressCandidateResponse;
import com.donate.app.dto.AddressSearchRequest;
import com.donate.app.integration.nominatim.NominatimClient;
import com.donate.app.integration.nominatim.NominatimClientException;
import com.donate.app.integration.nominatim.NominatimPlaceResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocationSearchService {

    private final NominatimClient nominatimClient;

    public LocationSearchService(NominatimClient nominatimClient) {
        this.nominatimClient = nominatimClient;
    }

    @Cacheable(
            cacheNames = CacheConfig.LOCATION_SEARCH_CACHE,
            key = "#root.target.buildCacheKey(#request)"
    )
    public List<AddressCandidateResponse> search(AddressSearchRequest request) {
        String query = buildQuery(request);

        try {
            return nominatimClient.search(query).stream()
                    .map(this::toCandidate)
                    .toList();
        } catch (NominatimClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Location search provider is unavailable",
                    ex
            );
        }
    }

    String buildQuery(AddressSearchRequest request) {
        List<String> parts = new ArrayList<>();

        addIfPresent(parts, request.nombreInstitucion());

        String streetLine = joinNonBlank(request.calle(), request.numeracion(), " ");
        addIfPresent(parts, streetLine);

        addIfPresent(parts, request.localidad());
        addIfPresent(parts, request.provincia());
        parts.add("Argentina");

        return String.join(", ", parts);
    }

    public String buildCacheKey(AddressSearchRequest request) {
        return normalizeKey(buildQuery(request));
    }

    AddressCandidateResponse toCandidate(NominatimPlaceResponse place) {
        Map<String, String> address = place.address() != null ? place.address() : Map.of();

        return new AddressCandidateResponse(
                place.placeId() != null ? place.placeId().toString() : null,
                resolveInstitutionName(place.displayName(), address),
                place.displayName(),
                firstNonBlank(address.get("state")),
                resolveLocality(address),
                parseCoordinate(place.lat()),
                parseCoordinate(place.lon())
        );
    }

    private String resolveInstitutionName(String displayName, Map<String, String> address) {
        String amenity = firstNonBlank(
                address.get("amenity"),
                address.get("hospital"),
                address.get("clinic"),
                address.get("healthcare")
        );
        if (amenity != null) {
            return amenity;
        }

        if (displayName != null && displayName.contains(",")) {
            return displayName.substring(0, displayName.indexOf(',')).trim();
        }

        return displayName != null && !displayName.isBlank() ? displayName.trim() : null;
    }

    private String resolveLocality(Map<String, String> address) {
        return firstNonBlank(
                address.get("city"),
                address.get("town"),
                address.get("village"),
                address.get("municipality"),
                address.get("suburb"),
                address.get("county")
        );
    }

    private BigDecimal parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private String joinNonBlank(String first, String second, String separator) {
        List<String> values = new ArrayList<>();
        addIfPresent(values, first);
        addIfPresent(values, second);
        return values.stream().collect(Collectors.joining(separator));
    }

    @SafeVarargs
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");

        return normalized;
    }
}
