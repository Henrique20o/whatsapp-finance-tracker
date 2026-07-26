package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
import com.wa.finance.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Transacao processarTransacaoDaFila(TransacaoRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByTelefone(dto.telefone())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para o telefone: " + dto.telefone()));

        Categoria categoria = categoriaRepository
                .findByNomeIgnoreCaseAndUsuarioId(dto.categoriaNome(), usuario.getId())
                .orElseGet(() -> {
                    Categoria novaCategoria = new Categoria();
                    novaCategoria.setNome(dto.categoriaNome());
                    novaCategoria.setAtiva(true);
                    novaCategoria.setUsuario(usuario);

                    return categoriaRepository.save(novaCategoria);
                });

        Transacao transacao = new Transacao();
        transacao.setValor(dto.valor());
        transacao.setDescricao(dto.descricao());
        transacao.setCategoria(categoria);
        transacao.setUsuario(usuario);

        transacaoRepository.save(transacao);
        return transacao;
    }
}