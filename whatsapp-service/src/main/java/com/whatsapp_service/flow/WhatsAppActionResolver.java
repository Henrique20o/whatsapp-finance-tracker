package com.whatsapp_service.flow;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Component
public class WhatsAppActionResolver {

    private static final String CANCEL_PREFIX = "cancelar_transacao_";
    private static final Set<String> MENU_COMMANDS = Set.of(
            "oi", "ola", "menu", "inicio", "comecar"
    );

    public ResolvedWhatsAppAction resolver(String texto) {
        String comando = normalizar(texto);

        if (comando.startsWith(CANCEL_PREFIX)) {
            return new ResolvedWhatsAppAction(
                    WhatsAppAction.CANCELAR_TRANSACAO,
                    extrairTransacaoId(comando)
            );
        }

        if (MENU_COMMANDS.contains(comando)) {
            return ResolvedWhatsAppAction.of(WhatsAppAction.ABRIR_MENU);
        }

        return switch (comando) {
            case "registrar gasto", "registrar_gasto" ->
                    ResolvedWhatsAppAction.of(WhatsAppAction.REGISTRAR_GASTO);
            case "ver relatorio", "ver_relatorio" ->
                    ResolvedWhatsAppAction.of(WhatsAppAction.VER_RELATORIO);
            case "mais opcoes", "mais_opcoes" ->
                    ResolvedWhatsAppAction.of(WhatsAppAction.MAIS_OPCOES);
            case "gerenciar categorias", "gerenciar_categorias" ->
                    ResolvedWhatsAppAction.of(WhatsAppAction.GERENCIAR_CATEGORIAS);
            case "listar categorias", "listar_categorias" ->
                    ResolvedWhatsAppAction.of(WhatsAppAction.LISTAR_CATEGORIAS);
            case "criar categoria", "criar_categoria" ->
                    ResolvedWhatsAppAction.of(WhatsAppAction.CRIAR_CATEGORIA);
            case "ajuda" -> ResolvedWhatsAppAction.of(WhatsAppAction.AJUDA);
            case "voltar ao menu", "voltar_menu" ->
                    ResolvedWhatsAppAction.of(WhatsAppAction.VOLTAR_MENU);
            default -> ResolvedWhatsAppAction.of(WhatsAppAction.TEXTO_LIVRE);
        };
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private Long extrairTransacaoId(String comando) {
        try {
            return Long.valueOf(comando.substring(CANCEL_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Identificador de cancelamento inválido", e);
        }
    }
}
