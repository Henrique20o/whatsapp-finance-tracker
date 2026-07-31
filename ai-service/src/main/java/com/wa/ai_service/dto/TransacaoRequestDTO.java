package com.wa.ai_service.dto;

import java.math.BigDecimal;

public record TransacaoRequestDTO(
        String telefone,
        BigDecimal valor,
        String descricao,
        String categoriaNome
) {}