package com.whatsapp_service.dto;

import java.math.BigDecimal;

public record GastoPorCategoriaDTO(
        String categoria,
        BigDecimal total
) {}
