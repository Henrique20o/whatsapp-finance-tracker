package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.dto.CancelamentoTransacaoDTO;
import com.wa.finance.producer.WhatsAppResponseProducer;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioService usuarioService;
    private final WhatsAppResponseProducer responseProducer;

    @Transactional
    public Transacao processarTransacaoDaFila(TransacaoRequestDTO dto) {

        if (dto.messageId() != null && !dto.messageId().isBlank()) {
            var transacaoExistente = transacaoRepository.findByExternalMessageId(dto.messageId());
            if (transacaoExistente.isPresent()) {
                log.info("Mensagem {} jÃ¡ processada; transaÃ§Ã£o duplicada ignorada", dto.messageId());
                return transacaoExistente.get();
            }
        }

        Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(dto.telefone());

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
        transacao.setExternalMessageId(dto.messageId());
        transacao.setValor(dto.valor());
        transacao.setDescricao(dto.descricao());
        transacao.setCategoria(categoria);
        transacao.setUsuario(usuario);

        transacaoRepository.saveAndFlush(transacao);

        responseProducer.enviar(
                new RespostaUsuarioDTO(
                        usuario.getTelefone(),
                        String.format(
                                "✅ *Gasto Registrado!*\n\n" +
                                        "💰 Valor: R$ %.2f\n" +
                                        "📂 Categoria: %s%s",
                                transacao.getValor(),
                                categoria.getNome(),
                                transacao.getDescricao() != null && !transacao.getDescricao().isBlank()
                                        ? "\n📝 Descrição: " + transacao.getDescricao()
                                        : ""
                        ),
                        transacao.getId()
                )
        );
        return transacao;
    }

    @Transactional
    public CancelamentoTransacaoDTO cancelar(Long transacaoId, String telefone) {
        Transacao transacao = transacaoRepository
                .findByIdAndUsuarioTelefone(transacaoId, telefone)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Transação não encontrada para o usuário"
                ));

        boolean canceladaAgora = !Boolean.TRUE.equals(transacao.getDeletado());

        if (canceladaAgora) {
            transacao.setDeletado(true);
            transacaoRepository.save(transacao);
        }

        return new CancelamentoTransacaoDTO(
                transacao.getId(),
                transacao.getValor(),
                canceladaAgora
        );
    }
}
