package com.whatsapp_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendImageRequest(
        @JsonProperty("Phone") String phone,
        @JsonProperty("Caption") String caption,
        @JsonProperty("Image") String image
) {}
