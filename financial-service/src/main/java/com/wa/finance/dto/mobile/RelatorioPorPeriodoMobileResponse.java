package com.wa.finance.dto.mobile;

import java.time.LocalDate;
import java.util.List;

public record RelatorioPorPeriodoMobileResponse(LocalDate dataInicio, LocalDate dataFim,
                                                 List<PontoRelatorioMobile> pontos) { }
