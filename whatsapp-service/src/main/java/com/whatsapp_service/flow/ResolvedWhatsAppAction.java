package com.whatsapp_service.flow;

public record ResolvedWhatsAppAction(
        WhatsAppAction action,
        Long transacaoId,
        String parametro
) {
    public static ResolvedWhatsAppAction of(WhatsAppAction action) {
        return new ResolvedWhatsAppAction(action, null, null);
    }

    public static ResolvedWhatsAppAction withParameter(WhatsAppAction action, String parametro) {
        return new ResolvedWhatsAppAction(action, null, parametro);
    }
}
