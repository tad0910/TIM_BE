package com.tim.appTim.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class KeycloakIntrospectionService {

    @Value("${keycloak.introspectUrl}")
    private String introspectionUri;

    @Value("${keycloak.client-id-app}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean introspectToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("token", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    introspectionUri,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            Object active = response.getBody().get("active");
            boolean isActive = active != null && Boolean.TRUE.equals(active);
            System.out.println("🔍 Token active: " + isActive);
            return isActive;
        } catch (Exception e) {
            System.err.println("❌ [KeycloakIntrospection] Error introspecting token: " + e.getMessage());
            return false;
        }
    }
}
