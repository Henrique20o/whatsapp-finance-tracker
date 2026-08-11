package com.whatsapp_service.service;

import com.whatsapp_service.dto.GastoPorCategoriaDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PieChartServiceTest {

    private final PieChartService service = new PieChartService();

    @Test
    void deveGerarImagemPngEmBase64() {
        String imagem = service.gerarGraficoBase64(List.of(
                new GastoPorCategoriaDTO("Alimentação", new BigDecimal("400.00")),
                new GastoPorCategoriaDTO("Lazer", new BigDecimal("220.90"))
        ));

        assertThat(imagem).startsWith("data:image/png;base64,");
        byte[] png = Base64.getDecoder().decode(imagem.substring("data:image/png;base64,".length()));
        assertThat(png).startsWith(0x89, 0x50, 0x4E, 0x47);
        assertThat(png.length).isGreaterThan(5_000);
    }

    @Test
    void deveRecusarGraficoSemGastos() {
        assertThatThrownBy(() -> service.gerarGraficoBase64(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
