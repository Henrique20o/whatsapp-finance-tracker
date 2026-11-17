package com.wa.finance.controller.mobile;

import com.wa.finance.dto.mobile.ResumoMobileResponse;
import com.wa.finance.dto.mobile.RelatorioPorPeriodoMobileResponse;
import com.wa.finance.security.MobilePrincipal;
import com.wa.finance.service.MobileReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/mobile/v1/relatorios")
public class MobileReportController {
    private final MobileReportService service;

    public MobileReportController(MobileReportService service) { this.service = service; }

    @GetMapping("/resumo")
    public ResumoMobileResponse resumo(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return service.resumo(MobilePrincipal.usuarioId(jwt), dataInicio, dataFim);
    }

    @GetMapping("/por-periodo")
    public RelatorioPorPeriodoMobileResponse porPeriodo(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return service.porPeriodo(MobilePrincipal.usuarioId(jwt), dataInicio, dataFim);
    }
}
