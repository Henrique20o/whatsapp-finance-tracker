package com.whatsapp_service.dto;

public record MensagemFilaDTO(
        String messageId,
        String telefone,
        String tipoMidia,
        String conteudo
) {}
