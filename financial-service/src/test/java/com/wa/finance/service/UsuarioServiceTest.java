package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Usuario;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveRepararCategoriasPadraoAusentesDeUsuarioExistente() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .telefone("5531999998888")
                .build();

        when(usuarioRepository.findByTelefone(usuario.getTelefone())).thenReturn(Optional.of(usuario));
        when(categoriaRepository.existsByNomeIgnoreCaseAndUsuarioId(anyString(), eq(1L))).thenReturn(false);

        Usuario resultado = usuarioService.buscarOuCriarUsuarioPorTelefone(usuario.getTelefone());

        ArgumentCaptor<Categoria> categoriasSalvas = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository, times(12)).save(categoriasSalvas.capture());

        assertThat(resultado).isSameAs(usuario);
        assertThat(categoriasSalvas.getAllValues())
                .extracting(Categoria::getNome)
                .contains("Alimenta\u00e7\u00e3o", "Lazer", "Transporte", "Outros");
        assertThat(categoriasSalvas.getAllValues())
                .allSatisfy(categoria -> {
                    assertThat(categoria.getUsuario()).isSameAs(usuario);
                    assertThat(categoria.getAtiva()).isTrue();
                });
    }

    @Test
    void naoDeveDuplicarCategoriasPadraoExistentes() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .telefone("5531999998888")
                .build();

        when(usuarioRepository.findByTelefone(usuario.getTelefone())).thenReturn(Optional.of(usuario));
        when(categoriaRepository.existsByNomeIgnoreCaseAndUsuarioId(anyString(), eq(1L))).thenReturn(true);

        usuarioService.buscarOuCriarUsuarioPorTelefone(usuario.getTelefone());

        verify(categoriaRepository, never()).save(org.mockito.ArgumentMatchers.any(Categoria.class));
    }
}
