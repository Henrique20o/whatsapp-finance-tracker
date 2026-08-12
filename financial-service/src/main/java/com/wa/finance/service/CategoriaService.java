package com.wa.finance.service;

import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioService usuarioService;

    public List<String> buscarNomesCategoriasPorTelefone(String telefone) {
        usuarioService.buscarOuCriarUsuarioPorTelefone(telefone);
        return categoriaRepository.findNomesCategoriasAtivasByTelefone(telefone);
    }

    @Transactional
    public String criarOuReativar(String telefone, String nomeInformado) {
        if (nomeInformado == null || nomeInformado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da categoria é obrigatório");
        }

        String nome = nomeInformado.trim().replaceAll("\\s+", " ");
        if (nome.length() < 2 || nome.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome deve ter entre 2 e 50 caracteres");
        }

        Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(telefone);
        Categoria categoria = categoriaRepository
                .findByNomeIgnoreCaseAndUsuarioId(nome, usuario.getId())
                .orElseGet(() -> Categoria.builder()
                        .nome(nome)
                        .ativa(true)
                        .usuario(usuario)
                        .build());

        categoria.setAtiva(true);
        return categoriaRepository.save(categoria).getNome();
    }

    @Transactional
    public String desativar(String telefone, String nomeInformado) {
        Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(telefone);
        Categoria categoria = categoriaRepository
                .findByNomeIgnoreCaseAndUsuarioId(nomeInformado.trim(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Categoria não encontrada"
                ));

        if ("Outros".equalsIgnoreCase(categoria.getNome())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A categoria Outros não pode ser desativada"
            );
        }

        if (!categoria.getAtiva()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A categoria já está desativada");
        }

        categoria.setAtiva(false);
        return categoriaRepository.save(categoria).getNome();
    }
}
