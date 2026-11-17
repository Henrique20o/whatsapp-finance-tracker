package com.wa.finance.dto.mobile;

import com.wa.finance.domain.Transacao;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record TransacaoMobileResponse(
        Long id,
        BigDecimal valor,
        String descricao,
        String dataHora,
        CategoriaMobileResponse categoria
) {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    public static TransacaoMobileResponse from(Transacao transacao) {
        String dataHora = transacao.getDataHora().atZone(BUSINESS_ZONE)
                .toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new TransacaoMobileResponse(
                transacao.getId(), transacao.getValor(), transacao.getDescricao(), dataHora,
                new CategoriaMobileResponse(transacao.getCategoria().getId(), transacao.getCategoria().getNome()));
    }
}
