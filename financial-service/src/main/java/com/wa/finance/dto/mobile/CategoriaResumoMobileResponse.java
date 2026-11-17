package com.wa.finance.dto.mobile;

import java.math.BigDecimal;

public record CategoriaResumoMobileResponse(
        Long categoriaId, String nome, BigDecimal total, long quantidadeGastos, BigDecimal percentual
) { }
