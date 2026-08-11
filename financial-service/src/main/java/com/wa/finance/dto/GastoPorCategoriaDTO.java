package com.wa.finance.dto;

import java.math.BigDecimal;

public record GastoPorCategoriaDTO(
        String categoria,
        BigDecimal total
) {}
