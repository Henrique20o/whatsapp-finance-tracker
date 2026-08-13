package com.wa.ai_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FinancialClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    public FinancialClient(RestClient.Builder builder,
                           @Value("${app.financial-service.url}") String financialServiceUrl,
                           @Value("${app.financial-service.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(financialServiceUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, apiKey)
                .build();
    }

    public List<String> buscarCategoriasPorTelefone(String telefone) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/categorias")
                        .queryParam("telefone", telefone)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});
    }
}
