package com.wa.finance.service;

import com.wa.finance.repository.TransacaoRepository;
import com.wa.finance.dto.GastoPorCategoriaDTO;
import com.wa.finance.security.PhoneProtectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private PhoneProtectionService phoneProtectionService;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    void deveCalcularTotaisDeSeteETrintaDias() {
        String telefone = "5531999998888";
        String telefoneHash = "hash-telefone";
        when(phoneProtectionService.lookupHash(telefone)).thenReturn(telefoneHash);
        when(transacaoRepository.somarGastosDesde(
                org.mockito.ArgumentMatchers.eq(telefoneHash),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(new BigDecimal("150.50"), new BigDecimal("620.90"));
        when(transacaoRepository.somarGastosPorCategoriaDesde(
                org.mockito.ArgumentMatchers.eq(telefoneHash),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(List.of(
                new GastoPorCategoriaDTO("Alimentação", new BigDecimal("400.00")),
                new GastoPorCategoriaDTO("Lazer", new BigDecimal("220.90"))
        ));

        var resumo = relatorioService.gerarResumo(telefone);

        assertThat(resumo.totalSeteDias()).isEqualByComparingTo("150.50");
        assertThat(resumo.totalTrintaDias()).isEqualByComparingTo("620.90");
        assertThat(resumo.gastosPorCategoria()).hasSize(2);
        assertThat(resumo.gastosPorCategoria().getFirst().categoria()).isEqualTo("Alimentação");

        ArgumentCaptor<LocalDateTime> inicios = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transacaoRepository, org.mockito.Mockito.times(2))
                .somarGastosDesde(org.mockito.ArgumentMatchers.eq(telefoneHash), inicios.capture());
        assertThat(inicios.getAllValues().get(0)).isAfter(inicios.getAllValues().get(1));
    }
}
