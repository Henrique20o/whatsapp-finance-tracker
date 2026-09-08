package com.whatsapp_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoRelatorioDTO(
        Long id,
        BigDecimal valor,
        String descricao,
        LocalDateTime dataHora
) {}
