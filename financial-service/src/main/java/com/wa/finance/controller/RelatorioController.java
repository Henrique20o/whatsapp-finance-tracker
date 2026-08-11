package com.wa.finance.controller;

import com.wa.finance.dto.ResumoFinanceiroDTO;
import com.wa.finance.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/resumo")
    public ResponseEntity<ResumoFinanceiroDTO> obterResumo(@RequestParam String telefone) {
        return ResponseEntity.ok(relatorioService.gerarResumo(telefone));
    }
}
