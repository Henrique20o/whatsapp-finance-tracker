package com.whatsapp_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SendTemplateRequest(
        @JsonProperty("Phone") String phone,
        @JsonProperty("Body") String body,
        @JsonProperty("Title") String title,
        @JsonProperty("Footer") String footer,
        @JsonProperty("Buttons") List<Button> buttons
) {
    public record Button(
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("id") String id
    ) {}
}
