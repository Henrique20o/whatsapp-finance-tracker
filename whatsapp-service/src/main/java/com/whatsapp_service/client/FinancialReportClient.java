package com.whatsapp_service.client;

import com.whatsapp_service.dto.ResumoFinanceiroDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinancialReportClient {

    private final RestClient restClient;

    public FinancialReportClient(
            RestClient.Builder builder,
            @Value("${app.financial-service.url}") String financialServiceUrl
    ) {
        this.restClient = builder.baseUrl(financialServiceUrl).build();
    }

    public ResumoFinanceiroDTO buscarResumo(String telefone) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/relatorios/resumo")
                        .queryParam("telefone", telefone)
                        .build())
                .retrieve()
                .body(ResumoFinanceiroDTO.class);
    }
}
