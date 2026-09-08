package com.wa.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioPeriodoDTO(
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal total,
        List<GastoPorCategoriaDTO> gastosPorCategoria
) {}
