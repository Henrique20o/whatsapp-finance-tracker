package com.wa.finance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MobileDailyTotalProjection {
    LocalDate getData();
    BigDecimal getTotal();
    long getQuantidade();
}
