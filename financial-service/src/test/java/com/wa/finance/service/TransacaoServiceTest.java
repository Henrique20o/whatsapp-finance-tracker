package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.producer.WhatsAppResponseProducer;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
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
    private WhatsAppResponseProducer responseProducer;

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
        verify(responseProducer, never()).enviar(any());
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
        when(categoriaRepository.findByNomeIgnoreCaseAndUsuarioId(dto.categoriaNome(), usuario.getId()))
                .thenReturn(Optional.of(categoria));
        when(transacaoRepository.saveAndFlush(any(Transacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transacao resultado = transacaoService.processarTransacaoDaFila(dto);

        assertThat(resultado.getExternalMessageId()).isEqualTo(dto.messageId());
        assertThat(resultado.getUsuario()).isSameAs(usuario);
        assertThat(resultado.getCategoria()).isSameAs(categoria);

        ArgumentCaptor<RespostaUsuarioDTO> resposta = ArgumentCaptor.forClass(RespostaUsuarioDTO.class);
        verify(responseProducer).enviar(resposta.capture());
        assertThat(resposta.getValue().telefone()).isEqualTo(dto.telefone());
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
