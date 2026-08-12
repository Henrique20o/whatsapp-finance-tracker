package com.wa.finance.service;

import com.wa.finance.dto.ResumoFinanceiroDTO;
import com.wa.finance.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
}
