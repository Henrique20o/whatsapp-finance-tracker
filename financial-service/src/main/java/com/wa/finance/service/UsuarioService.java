package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Usuario;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.UsuarioRepository;
import com.wa.finance.security.PhoneProtectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final PhoneProtectionService phoneProtectionService;

    @Transactional
    public Usuario buscarOuCriarUsuarioPorTelefone(String telefone) {
        String normalizado = phoneProtectionService.normalize(telefone);
        String hash = phoneProtectionService.lookupHash(normalizado);
        Usuario usuario = usuarioRepository.findByTelefoneHash(hash)
                .orElseGet(() -> criarNovoUsuario(normalizado, hash));

        usuario.setTelefone(normalizado);

        garantirCategoriasPadrao(usuario);
        return usuario;
    }

    private Usuario criarNovoUsuario(String telefone, String telefoneHash) {
        Usuario novoUsuario = Usuario.builder()
                .telefone(telefone)
                .telefoneCriptografado(phoneProtectionService.encrypt(telefone))
                .telefoneHash(telefoneHash)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);
        return usuarioSalvo;
    }

    public String obterTelefone(Usuario usuario) {
        if (usuario.getTelefone() != null) {
            return phoneProtectionService.normalize(usuario.getTelefone());
        }
        String telefone = phoneProtectionService.decrypt(usuario.getTelefoneCriptografado());
        usuario.setTelefone(telefone);
        return telefone;
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
