package com.donate.app.service;

import com.donate.app.dto.AddressCandidateResponse;
import com.donate.app.dto.AddressSearchRequest;
import com.donate.app.integration.nominatim.NominatimClient;
import com.donate.app.integration.nominatim.NominatimClientException;
import com.donate.app.integration.nominatim.NominatimPlaceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationSearchServiceTest {

    @Mock
    private NominatimClient nominatimClient;

    private LocationSearchService locationSearchService;

    @BeforeEach
    void setUp() {
        locationSearchService = new LocationSearchService(nominatimClient);
    }

    @Test
    void buildQueryUsesExpectedOrderAndOmitsBlankInstitution() {
        AddressSearchRequest request = new AddressSearchRequest(
                " ",
                "Buenos Aires",
                "La Plata",
                "Calle 7",
                "1234"
        );

        String query = locationSearchService.buildQuery(request);

        assertThat(query).isEqualTo("Calle 7 1234, La Plata, Buenos Aires, Argentina");
    }

    @Test
    void buildQueryIncludesInstitutionWhenPresent() {
        AddressSearchRequest request = new AddressSearchRequest(
                "Hospital Italiano",
                "Ciudad Autónoma de Buenos Aires",
                "Buenos Aires",
                "Tte. Gral. Juan Domingo Perón",
                "4190"
        );

        String query = locationSearchService.buildQuery(request);

        assertThat(query).isEqualTo(
                "Hospital Italiano, Tte. Gral. Juan Domingo Perón 4190, Buenos Aires, Ciudad Autónoma de Buenos Aires, Argentina"
        );
    }

    @Test
    void searchReturnsEmptyListWhenNominatimHasNoMatches() {
        when(nominatimClient.search(anyString())).thenReturn(List.of());

        List<AddressCandidateResponse> results = locationSearchService.search(sampleRequest());

        assertThat(results).isEmpty();
        verify(nominatimClient).search(anyString());
    }

    @Test
    void searchMapsNominatimFieldsSafelyWhenAddressIsPartial() {
        when(nominatimClient.search(anyString())).thenReturn(List.of(
                new NominatimPlaceResponse(
                        987L,
                        "Hospital Central, Calle 1, La Plata, Buenos Aires, Argentina",
                        "-34.9205",
                        "-57.9536",
                        Map.of("state", "Buenos Aires", "city", "La Plata")
                )
        ));

        AddressCandidateResponse candidate = locationSearchService.search(sampleRequest()).get(0);

        assertThat(candidate.externalId()).isEqualTo("987");
        assertThat(candidate.formattedAddress()).contains("Hospital Central");
        assertThat(candidate.province()).isEqualTo("Buenos Aires");
        assertThat(candidate.locality()).isEqualTo("La Plata");
        assertThat(candidate.latitude()).isEqualByComparingTo("-34.9205");
        assertThat(candidate.longitude()).isEqualByComparingTo("-57.9536");
    }

    @Test
    void searchHandlesMissingAddressFields() {
        when(nominatimClient.search(anyString())).thenReturn(List.of(
                new NominatimPlaceResponse(
                        1L,
                        "Some place",
                        "10.5",
                        "20.5",
                        null
                )
        ));

        AddressCandidateResponse candidate = locationSearchService.search(sampleRequest()).get(0);

        assertThat(candidate.province()).isNull();
        assertThat(candidate.locality()).isNull();
        assertThat(candidate.institutionName()).isEqualTo("Some place");
    }

    @Test
    void searchWrapsExternalServiceFailure() {
        when(nominatimClient.search(anyString()))
                .thenThrow(new NominatimClientException("timeout", new RuntimeException("timeout")));

        assertThatThrownBy(() -> locationSearchService.search(sampleRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Location search provider is unavailable");
    }

    @Test
    void buildCacheKeyNormalizesAccentsAndSpacing() {
        AddressSearchRequest first = new AddressSearchRequest(
                null,
                "Córdoba",
                "Córdoba Capital",
                "San Martín",
                "100"
        );
        AddressSearchRequest second = new AddressSearchRequest(
                null,
                "  CORDOBA  ",
                "cordoba capital",
                "san   martin",
                "100"
        );

        assertThat(locationSearchService.buildCacheKey(first))
                .isEqualTo(locationSearchService.buildCacheKey(second));
    }

    private AddressSearchRequest sampleRequest() {
        return new AddressSearchRequest(
                "Hospital Italiano",
                "Buenos Aires",
                "La Plata",
                "Calle 7",
                "1234"
        );
    }
}
