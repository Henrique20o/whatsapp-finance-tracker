package com.whatsapp_service.client;

import com.whatsapp_service.dto.ResumoFinanceiroDTO;
import com.whatsapp_service.dto.CancelamentoTransacaoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

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

    public CancelamentoTransacaoDTO cancelarTransacao(Long transacaoId, String telefone) {
        return restClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/transacoes/{id}")
                        .queryParam("telefone", telefone)
                        .build(transacaoId))
                .retrieve()
                .body(CancelamentoTransacaoDTO.class);
    }

    public List<String> buscarCategorias(String telefone) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/categorias")
                        .queryParam("telefone", telefone)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
