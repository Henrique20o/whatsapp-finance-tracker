package com.wa.finance.service;

import com.wa.finance.dto.ResumoFinanceiroDTO;
import com.wa.finance.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final TransacaoRepository transacaoRepository;

    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarResumo(String telefone) {
        LocalDateTime agora = LocalDateTime.now(BUSINESS_ZONE);
        LocalDateTime inicioTrintaDias = agora.minusDays(30);

        return new ResumoFinanceiroDTO(
                transacaoRepository.somarGastosDesde(telefone, agora.minusDays(7)),
                transacaoRepository.somarGastosDesde(telefone, inicioTrintaDias),
                transacaoRepository.somarGastosPorCategoriaDesde(telefone, inicioTrintaDias)
        );
    }
}
