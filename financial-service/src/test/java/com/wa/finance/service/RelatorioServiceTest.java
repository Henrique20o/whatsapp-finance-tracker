package com.wa.finance.service;

import com.wa.finance.repository.TransacaoRepository;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.dto.GastoPorCategoriaDTO;
import com.wa.finance.security.PhoneProtectionService;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Categoria;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

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
                new GastoPorCategoriaDTO(1L, "Alimentação", new BigDecimal("400.00")),
                new GastoPorCategoriaDTO(2L, "Lazer", new BigDecimal("220.90"))
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

    @Test
    void deveGerarRelatorioComIntervaloFechadoAberto() {
        String telefone = "5531999998888";
        String telefoneHash = "hash-telefone";
        LocalDate inicio = LocalDate.of(2026, 7, 1);
        LocalDate fim = LocalDate.of(2026, 7, 31);
        when(phoneProtectionService.lookupHash(telefone)).thenReturn(telefoneHash);
        when(transacaoRepository.somarGastosNoPeriodo(
                telefoneHash, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay()))
                .thenReturn(new BigDecimal("620.90"));
        when(transacaoRepository.somarGastosPorCategoriaNoPeriodo(
                telefoneHash, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(new GastoPorCategoriaDTO(2L, "Lazer", new BigDecimal("620.90"))));

        var relatorio = relatorioService.gerarRelatorioPorPeriodo(telefone, inicio, fim);

        assertThat(relatorio.dataInicio()).isEqualTo(inicio);
        assertThat(relatorio.dataFim()).isEqualTo(fim);
        assertThat(relatorio.total()).isEqualByComparingTo("620.90");
        assertThat(relatorio.gastosPorCategoria()).extracting(GastoPorCategoriaDTO::categoria)
                .containsExactly("Lazer");
    }

    @Test
    void deveDetalharCategoriaComPaginacaoERestringirPeloTelefone() {
        String telefone = "5531999998888";
        String telefoneHash = "hash-telefone";
        LocalDate inicio = LocalDate.of(2026, 7, 1);
        LocalDate fim = LocalDate.of(2026, 7, 31);
        Transacao transacao = Transacao.builder()
                .id(42L)
                .valor(new BigDecimal("50.00"))
                .descricao("Jogo de futebol")
                .dataHora(LocalDateTime.of(2026, 7, 12, 20, 30))
                .build();

        when(phoneProtectionService.lookupHash(telefone)).thenReturn(telefoneHash);
        when(categoriaRepository.findByIdAndUsuarioTelefoneHash(2L, telefoneHash))
                .thenReturn(java.util.Optional.of(Categoria.builder().id(2L).nome("Lazer").build()));
        when(transacaoRepository.buscarPorCategoriaNoPeriodo(
                org.mockito.ArgumentMatchers.eq(telefoneHash),
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(inicio.atStartOfDay()),
                org.mockito.ArgumentMatchers.eq(fim.plusDays(1).atStartOfDay()),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(transacao)));
        when(transacaoRepository.somarCategoriaNoPeriodo(
                telefoneHash, 2L, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay()))
                .thenReturn(new BigDecimal("50.00"));

        var detalhe = relatorioService.detalharCategoria(
                telefone, 2L, inicio, fim, 0, 10);

        assertThat(detalhe.categoria()).isEqualTo("Lazer");
        assertThat(detalhe.total()).isEqualByComparingTo("50.00");
        assertThat(detalhe.quantidade()).isEqualTo(1);
        assertThat(detalhe.transacoes()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(42L);
            assertThat(item.descricao()).isEqualTo("Jogo de futebol");
        });
    }
}
