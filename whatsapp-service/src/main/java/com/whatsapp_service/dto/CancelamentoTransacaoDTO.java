package com.whatsapp_service.dto;

import java.math.BigDecimal;

public record CancelamentoTransacaoDTO(
        Long transacaoId,
        BigDecimal valor,
        boolean canceladaAgora
) {}
