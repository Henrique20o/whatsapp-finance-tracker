package com.whatsapp_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendTextRequest(

        @JsonProperty("Phone")
        String phone,

        @JsonProperty("Body")
        String body

) {}