package com.wa.finance.controller;

import com.wa.finance.domain.Transacao;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.dto.CancelamentoTransacaoDTO;
import com.wa.finance.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<Transacao> registrarTransacao(@Valid @RequestBody TransacaoRequestDTO dto) {
        Transacao transacaoSalva = transacaoService.processarTransacaoDaFila(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(transacaoSalva);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CancelamentoTransacaoDTO> cancelarTransacao(
            @PathVariable Long id,
            @RequestParam String telefone
    ) {
        return ResponseEntity.ok(transacaoService.cancelar(id, telefone));
    }
}
