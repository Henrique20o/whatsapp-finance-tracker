package com.wa.finance.dto;

import com.wa.finance.domain.Transacao;

import java.math.BigDecimal;

public record TransacaoResponseDTO(
        Long id,
        BigDecimal valor,
        String descricao,
        String categoria,
        String dataHora,
        boolean cancelada
) {

    public static TransacaoResponseDTO from(Transacao transacao) {
        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getValor(),
                transacao.getDescricao(),
                transacao.getCategoria().getNome(),
                transacao.getDataHora() == null ? null : transacao.getDataHora().toString(),
                Boolean.TRUE.equals(transacao.getDeletado())
        );
    }
}
