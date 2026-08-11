package com.wa.finance.dto;

public record RespostaUsuarioDTO(
        String telefone,
        String mensagem,
        Long transacaoIdCancelavel
) {}
