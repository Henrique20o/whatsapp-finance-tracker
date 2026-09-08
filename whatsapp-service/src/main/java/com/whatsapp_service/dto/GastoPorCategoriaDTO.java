package com.whatsapp_service.dto;

import java.math.BigDecimal;

public record GastoPorCategoriaDTO(
        Long categoriaId,
        String categoria,
        BigDecimal total
) {}
