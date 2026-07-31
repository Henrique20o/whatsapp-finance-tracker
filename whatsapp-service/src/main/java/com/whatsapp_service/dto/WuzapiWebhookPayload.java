package com.whatsapp_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WuzapiWebhookPayload(

        @JsonProperty("type")
        String type,

        @JsonProperty("event")
        Event event

) {

    public record Event(

            @JsonProperty("Info")
            Info info,

            @JsonProperty("Message")
            Message message

    ) {}


    public record Info(

            @JsonProperty("Sender")
            String sender,

            @JsonProperty("SenderAlt")
            String senderAlt,

            @JsonProperty("Chat")
            String chat

    ) {

        public String getTelefone() {

            String numero = senderAlt;

            if (numero == null || numero.isBlank()) {
                numero = sender;
            }

            if (numero == null || numero.isBlank()) {
                return null;
            }

            if (numero.endsWith("@lid")) {
                return null;
            }

            return numero
                    .replace("@s.whatsapp.net", "")
                    .trim();
        }
    }


    public record Message(

            @JsonProperty("conversation")
            String conversation

    ) {}
}