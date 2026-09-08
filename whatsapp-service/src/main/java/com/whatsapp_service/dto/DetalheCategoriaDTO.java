package com.whatsapp_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DetalheCategoriaDTO(
        LocalDate dataInicio,
        LocalDate dataFim,
        String categoria,
        BigDecimal total,
        long quantidade,
        int pagina,
        int totalPaginas,
        List<TransacaoRelatorioDTO> transacoes
) {}
