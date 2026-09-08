package com.wa.finance.service;

import com.wa.finance.dto.ResumoFinanceiroDTO;
import com.wa.finance.dto.RelatorioPeriodoDTO;
import com.wa.finance.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import com.wa.finance.security.PhoneProtectionService;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final TransacaoRepository transacaoRepository;
    private final PhoneProtectionService phoneProtectionService;

    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarResumo(String telefone) {
        LocalDateTime agora = LocalDateTime.now(BUSINESS_ZONE);
        LocalDateTime inicioTrintaDias = agora.minusDays(30);
        String telefoneHash = phoneProtectionService.lookupHash(telefone);

        return new ResumoFinanceiroDTO(
                transacaoRepository.somarGastosDesde(telefoneHash, agora.minusDays(7)),
                transacaoRepository.somarGastosDesde(telefoneHash, inicioTrintaDias),
                transacaoRepository.somarGastosPorCategoriaDesde(telefoneHash, inicioTrintaDias)
        );
    }

    @Transactional(readOnly = true)
    public RelatorioPeriodoDTO gerarRelatorioPorPeriodo(
            String telefone,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        if (dataInicio == null || dataFim == null || dataFim.isBefore(dataInicio)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "O período informado é inválido"
            );
        }
        if (dataInicio.plusYears(1).isBefore(dataFim)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "O período máximo permitido é de um ano"
            );
        }

        String telefoneHash = phoneProtectionService.lookupHash(telefone);
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fimExclusivo = dataFim.plusDays(1).atStartOfDay();

        return new RelatorioPeriodoDTO(
                dataInicio,
                dataFim,
                transacaoRepository.somarGastosNoPeriodo(telefoneHash, inicio, fimExclusivo),
                transacaoRepository.somarGastosPorCategoriaNoPeriodo(telefoneHash, inicio, fimExclusivo)
        );
    }
}
