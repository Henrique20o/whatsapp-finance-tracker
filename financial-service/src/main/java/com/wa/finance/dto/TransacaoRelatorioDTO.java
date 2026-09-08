package com.wa.finance.dto;

import com.wa.finance.domain.Transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoRelatorioDTO(
        Long id,
        BigDecimal valor,
        String descricao,
        LocalDateTime dataHora
) {
    public static TransacaoRelatorioDTO from(Transacao transacao) {
        return new TransacaoRelatorioDTO(
                transacao.getId(),
                transacao.getValor(),
                transacao.getDescricao(),
                transacao.getDataHora()
        );
    }
}
