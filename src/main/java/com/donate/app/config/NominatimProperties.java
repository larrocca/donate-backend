package com.donate.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nominatim")
public record NominatimProperties(
        String baseUrl,
        String userAgent,
        int maxResults,
        int connectTimeoutMs,
        int readTimeoutMs
) {

    public NominatimProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://nominatim.openstreetmap.org";
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalStateException(
                    "nominatim.user-agent is required. Example: DonateApp/1.0 (contact: your-email@domain.com)"
            );
        }
        if (userAgent.toLowerCase().contains("@example.com")) {
            throw new IllegalStateException(
                    "nominatim.user-agent must use a real contact email. Nominatim blocks @example.com addresses."
            );
        }
        if (maxResults <= 0) {
            maxResults = 5;
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 5000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 10000;
        }
    }
}
