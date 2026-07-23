package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public Transacao registrarTransacao(TransacaoRequestDTO dto) {
        Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(dto.telefone());

        Categoria categoria = categoriaRepository
                .findByNomeIgnoreCaseAndUsuarioIdAndAtivaTrue(dto.categoriaNome(), usuario.getId())
                .orElseGet(() -> categoriaRepository
                        .findByNomeIgnoreCaseAndUsuarioIdAndAtivaTrue("Outros", usuario.getId())
                        .orElseThrow(() -> new RuntimeException("Categoria 'Outros' não encontrada")));

        Transacao novaTransacao = Transacao.builder()
                .valor(dto.valor())
                .descricao(dto.descricao())
                .usuario(usuario)
                .categoria(categoria)
                .deletado(false)
                .build();

        return transacaoRepository.save(novaTransacao);
    }
}