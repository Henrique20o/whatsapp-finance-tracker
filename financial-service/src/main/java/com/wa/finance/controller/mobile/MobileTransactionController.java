package com.wa.finance.controller.mobile;

import com.wa.finance.dto.mobile.TransacoesMobilePageResponse;
import com.wa.finance.dto.mobile.CriarTransacaoMobileRequest;
import com.wa.finance.dto.mobile.TransacaoMobileResponse;
import com.wa.finance.security.MobilePrincipal;
import com.wa.finance.service.MobileTransactionQueryService;
import com.wa.finance.service.MobileTransactionWriteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/mobile/v1/transacoes")
public class MobileTransactionController {
    private final MobileTransactionQueryService queryService;
    private final MobileTransactionWriteService writeService;

    public MobileTransactionController(MobileTransactionQueryService queryService,
                                       MobileTransactionWriteService writeService) {
        this.queryService = queryService;
        this.writeService = writeService;
    }

    @GetMapping
    public TransacoesMobilePageResponse listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long categoriaId
    ) {
        return queryService.listar(MobilePrincipal.usuarioId(jwt), dataInicio, dataFim,
                categoriaId, pagina, tamanho);
    }

    @PostMapping
    public ResponseEntity<TransacaoMobileResponse> criar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CriarTransacaoMobileRequest request
    ) {
        TransacaoMobileResponse response = writeService.criar(
                MobilePrincipal.usuarioId(jwt), idempotencyKey, request);
        return ResponseEntity.created(java.net.URI.create("/mobile/v1/transacoes/" + response.id()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }
}
