package com.wa.finance.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaMobileRequest(@NotBlank @Size(max = 50) String nome) { }
