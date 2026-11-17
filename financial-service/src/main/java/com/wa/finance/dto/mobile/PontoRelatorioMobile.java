package com.wa.finance.dto.mobile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PontoRelatorioMobile(LocalDate data, BigDecimal total, long quantidadeGastos) { }
