package com.wa.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransacaoRequestDTO(
        String messageId,

        @NotBlank(message = "O telefone do usuário é obrigatório")
        String telefone,

        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotBlank(message = "O nome da categoria é obrigatório")
        String categoriaNome
) {}
