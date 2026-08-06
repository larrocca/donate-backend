package com.donate.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class Auth0UserInfoClient {

    private final RestClient restClient;
    private final String userInfoUri;

    public Auth0UserInfoClient(@Value("${auth0.domain}") String domain) {
        this.userInfoUri = "https://" + domain + "/userinfo";
        this.restClient = RestClient.create();
    }

    public Map<String, Object> fetchProfile(String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
