package com.donate.app.integration.nominatim;

import com.donate.app.config.NominatimProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
public class NominatimClient {

    private final RestClient restClient;
    private final NominatimProperties properties;

    public NominatimClient(NominatimProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .requestFactory(requestFactory)
                .build();
    }

    public List<NominatimPlaceResponse> search(String query) {
        try {
            List<NominatimPlaceResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .queryParam("limit", properties.maxResults())
                            .queryParam("countrycodes", "ar")
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response != null ? response : List.of();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 403) {
                throw new NominatimClientException(
                        "Nominatim rejected the request. Set nominatim.user-agent with a valid contact email.",
                        ex
                );
            }
            throw new NominatimClientException("Nominatim returned an invalid response", ex);
        } catch (RestClientException ex) {
            throw new NominatimClientException("Nominatim is unavailable", ex);
        }
    }
}
