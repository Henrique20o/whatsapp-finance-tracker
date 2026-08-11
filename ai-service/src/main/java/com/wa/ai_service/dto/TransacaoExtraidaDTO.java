package com.wa.ai_service.dto;

import java.math.BigDecimal;

public record TransacaoExtraidaDTO(
        String telefone,
        BigDecimal valor,
        String descricao,
        String categoriaNome
) {}
