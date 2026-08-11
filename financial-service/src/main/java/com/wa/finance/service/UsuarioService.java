package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Usuario;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional
    public Usuario buscarOuCriarUsuarioPorTelefone(String telefone) {
        Usuario usuario = usuarioRepository.findByTelefone(telefone)
                .orElseGet(() -> criarNovoUsuario(telefone));

        garantirCategoriasPadrao(usuario);
        return usuario;
    }

    private Usuario criarNovoUsuario(String telefone) {
        Usuario novoUsuario = Usuario.builder()
                .telefone(telefone)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);
        return usuarioSalvo;
    }

    private void garantirCategoriasPadrao(Usuario usuario) {
        List<String> categoriasPadrao = List.of(
                "Contas domésticas", "Alimentação", "Transporte", "Saúde e bem-estar", "Cuidado pessoal", "Lazer", "Educação", "Pets", "Doações e presentes", "Tecnologia", "Profissional", "Outros"
        );

        categoriasPadrao.forEach(nome -> {
            if (categoriaRepository.existsByNomeIgnoreCaseAndUsuarioId(nome, usuario.getId())) {
                return;
            }

            Categoria categoria = Categoria.builder()
                    .nome(nome)
                    .usuario(usuario)
                    .ativa(true)
                    .build();
            categoriaRepository.save(categoria);
        });
    }
}
