package com.wa.ai_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FinancialClient {

    private final RestClient restClient;

    public FinancialClient(RestClient.Builder builder,
                           @Value("${app.financial-service.url}") String financialServiceUrl) {
        this.restClient = builder.baseUrl(financialServiceUrl).build();
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