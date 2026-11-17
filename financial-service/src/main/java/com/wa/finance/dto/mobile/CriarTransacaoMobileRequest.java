package com.wa.finance.dto.mobile;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CriarTransacaoMobileRequest(
        @NotNull @Digits(integer = 13, fraction = 2)
        BigDecimal valor,
        @NotBlank @Size(max = 255) String descricao,
        @NotNull Long categoriaId,
        OffsetDateTime dataHora
) { }
