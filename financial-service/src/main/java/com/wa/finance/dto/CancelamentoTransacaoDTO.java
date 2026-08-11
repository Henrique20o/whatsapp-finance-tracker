package com.wa.finance.dto;

import java.math.BigDecimal;

public record CancelamentoTransacaoDTO(
        Long transacaoId,
        BigDecimal valor,
        boolean canceladaAgora
) {}
