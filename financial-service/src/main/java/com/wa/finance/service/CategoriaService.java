package com.wa.finance.service;

import com.wa.finance.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
}
