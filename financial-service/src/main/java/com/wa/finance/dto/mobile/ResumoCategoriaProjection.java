package com.wa.finance.dto.mobile;

import java.math.BigDecimal;

public record ResumoCategoriaProjection(Long categoriaId, String nome, BigDecimal total, long quantidade) { }
