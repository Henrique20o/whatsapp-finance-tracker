package com.whatsapp_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

public record WuzapiWebhookPayload(
        @JsonProperty("type") String type,
        @JsonProperty("event") JsonNode event
) {

    public record Event(
            @JsonProperty("Info") Info info,
            @JsonProperty("Message") Message message
    ) {}

    public record Info(
            @JsonProperty("ID") String id,
            @JsonProperty("Sender") String sender,
            @JsonProperty("SenderAlt") String senderAlt,
            @JsonProperty("Chat") String chat
    ) {
        public String getTelefone() {
            String numero = senderAlt;

            if (numero == null || numero.isBlank()) {
                numero = sender;
            }

            if (numero == null || numero.isBlank() || numero.endsWith("@lid")) {
                return null;
            }

            return numero.replace("@s.whatsapp.net", "").trim();
        }
    }

    public record Message(
            @JsonProperty("conversation") String conversation,
            @JsonProperty("templateButtonReplyMessage") ButtonReply templateButtonReplyMessage,
            @JsonProperty("buttonsResponseMessage") ButtonReply buttonsResponseMessage
    ) {
        public String getTexto() {
            if (conversation != null && !conversation.isBlank()) {
                return conversation;
            }

            if (templateButtonReplyMessage != null) {
                return templateButtonReplyMessage.getTexto();
            }

            return buttonsResponseMessage == null ? null : buttonsResponseMessage.getTexto();
        }
    }

    public record ButtonReply(
            @JsonProperty("selectedDisplayText") String selectedDisplayText,
            @JsonProperty("selectedButtonId") String selectedButtonId,
            @JsonProperty("selectedID") String selectedId
    ) {
        public String getTexto() {
            if (selectedId != null && !selectedId.isBlank()) {
                return selectedId;
            }

            if (selectedButtonId != null && !selectedButtonId.isBlank()) {
                return selectedButtonId;
            }

            return selectedDisplayText;
        }
    }
}
