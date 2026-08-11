package com.whatsapp_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoFinanceiroDTO(
        BigDecimal totalSeteDias,
        BigDecimal totalTrintaDias,
        List<GastoPorCategoriaDTO> gastosPorCategoria
) {}
