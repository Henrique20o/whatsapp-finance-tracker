package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.outbox.OutboxService;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
import com.wa.finance.security.PhoneProtectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private PhoneProtectionService phoneProtectionService;

    @InjectMocks
    private TransacaoService transacaoService;

    @Test
    void deveIgnorarMensagemJaProcessada() {
        TransacaoRequestDTO dto = novaRequisicao("message-id-duplicado");
        Transacao existente = Transacao.builder()
                .id(10L)
                .externalMessageId(dto.messageId())
                .build();

        when(transacaoRepository.findByExternalMessageId(dto.messageId()))
                .thenReturn(Optional.of(existente));

        Transacao resultado = transacaoService.processarTransacaoDaFila(dto);

        assertThat(resultado).isSameAs(existente);
        verify(usuarioService, never()).buscarOuCriarUsuarioPorTelefone(any());
        verify(transacaoRepository, never()).saveAndFlush(any());
        verify(outboxService, never()).adicionarConfirmacao(any());
    }

    @Test
    void devePersistirMessageIdAntesDeEnviarConfirmacao() {
        TransacaoRequestDTO dto = novaRequisicao("message-id-novo");
        Usuario usuario = Usuario.builder().id(1L).telefone(dto.telefone()).build();
        Categoria categoria = Categoria.builder()
                .id(2L)
                .nome(dto.categoriaNome())
                .usuario(usuario)
                .ativa(true)
                .build();

        when(transacaoRepository.findByExternalMessageId(dto.messageId())).thenReturn(Optional.empty());
        when(usuarioService.buscarOuCriarUsuarioPorTelefone(dto.telefone())).thenReturn(usuario);
        when(usuarioService.obterTelefone(usuario)).thenReturn(dto.telefone());
        when(categoriaRepository.findByNomeIgnoreCaseAndUsuarioId(dto.categoriaNome(), usuario.getId()))
                .thenReturn(Optional.of(categoria));
        when(transacaoRepository.saveAndFlush(any(Transacao.class)))
                .thenAnswer(invocation -> {
                    Transacao transacao = invocation.getArgument(0);
                    transacao.setId(99L);
                    return transacao;
                });

        Transacao resultado = transacaoService.processarTransacaoDaFila(dto);

        assertThat(resultado.getExternalMessageId()).isEqualTo(dto.messageId());
        assertThat(resultado.getUsuario()).isSameAs(usuario);
        assertThat(resultado.getCategoria()).isSameAs(categoria);

        ArgumentCaptor<RespostaUsuarioDTO> resposta = ArgumentCaptor.forClass(RespostaUsuarioDTO.class);
        verify(outboxService).adicionarConfirmacao(resposta.capture());
        assertThat(resposta.getValue().telefone()).isEqualTo(dto.telefone());
        assertThat(resposta.getValue().transacaoIdCancelavel()).isEqualTo(99L);
    }

    @Test
    void deveMontarRespostaDaApiSemExporUsuario() {
        TransacaoRequestDTO dto = novaRequisicao("message-id-api");
        Usuario usuario = Usuario.builder().id(1L).telefone(dto.telefone()).build();
        Categoria categoria = Categoria.builder()
                .id(2L)
                .nome(dto.categoriaNome())
                .usuario(usuario)
                .ativa(true)
                .build();

        when(transacaoRepository.findByExternalMessageId(dto.messageId())).thenReturn(Optional.empty());
        when(usuarioService.buscarOuCriarUsuarioPorTelefone(dto.telefone())).thenReturn(usuario);
        when(usuarioService.obterTelefone(usuario)).thenReturn(dto.telefone());
        when(categoriaRepository.findByNomeIgnoreCaseAndUsuarioId(dto.categoriaNome(), usuario.getId()))
                .thenReturn(Optional.of(categoria));
        when(transacaoRepository.saveAndFlush(any(Transacao.class)))
                .thenAnswer(invocation -> {
                    Transacao transacao = invocation.getArgument(0);
                    transacao.setId(99L);
                    return transacao;
                });

        var resposta = transacaoService.registrarViaApi(dto);

        assertThat(resposta.id()).isEqualTo(99L);
        assertThat(resposta.categoria()).isEqualTo("Lazer");
        assertThat(resposta.valor()).isEqualByComparingTo("50.00");
    }

    @Test
    void deveCancelarTransacaoDoProprioUsuario() {
        Usuario usuario = Usuario.builder().id(1L).telefone("5531999998888").build();
        Transacao transacao = Transacao.builder()
                .id(99L)
                .usuario(usuario)
                .valor(new BigDecimal("50.00"))
                .deletado(false)
                .build();
        when(phoneProtectionService.lookupHash(usuario.getTelefone())).thenReturn("hash-telefone");
        when(transacaoRepository.findByIdAndUsuarioTelefoneHash(99L, "hash-telefone"))
                .thenReturn(Optional.of(transacao));

        var resultado = transacaoService.cancelar(99L, usuario.getTelefone());

        assertThat(resultado.canceladaAgora()).isTrue();
        assertThat(transacao.getDeletado()).isTrue();
        verify(transacaoRepository).save(transacao);
    }

    @Test
    void deveSerIdempotenteAoCancelarNovamente() {
        Usuario usuario = Usuario.builder().id(1L).telefone("5531999998888").build();
        Transacao transacao = Transacao.builder()
                .id(99L)
                .usuario(usuario)
                .valor(new BigDecimal("50.00"))
                .deletado(true)
                .build();
        when(phoneProtectionService.lookupHash(usuario.getTelefone())).thenReturn("hash-telefone");
        when(transacaoRepository.findByIdAndUsuarioTelefoneHash(99L, "hash-telefone"))
                .thenReturn(Optional.of(transacao));

        var resultado = transacaoService.cancelar(99L, usuario.getTelefone());

        assertThat(resultado.canceladaAgora()).isFalse();
        verify(transacaoRepository, never()).save(any());
    }

    private TransacaoRequestDTO novaRequisicao(String messageId) {
        return new TransacaoRequestDTO(
                messageId,
                "5531999998888",
                new BigDecimal("50.00"),
                "Futebol",
                "Lazer"
        );
    }
}
