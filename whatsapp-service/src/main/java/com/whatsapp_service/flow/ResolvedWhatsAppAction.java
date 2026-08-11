package com.whatsapp_service.flow;

public record ResolvedWhatsAppAction(
        WhatsAppAction action,
        Long transacaoId
) {
    public static ResolvedWhatsAppAction of(WhatsAppAction action) {
        return new ResolvedWhatsAppAction(action, null);
    }
}
