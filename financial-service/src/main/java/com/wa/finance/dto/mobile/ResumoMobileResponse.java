package com.wa.finance.dto.mobile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoMobileResponse(
        LocalDate dataInicio, LocalDate dataFim, BigDecimal total,
        long quantidadeGastos, List<CategoriaResumoMobileResponse> categorias
) { }
