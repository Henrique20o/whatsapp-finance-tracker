package com.wa.finance.service;

import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.dto.CancelamentoTransacaoDTO;
import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.dto.TransacaoResponseDTO;
import com.wa.finance.outbox.OutboxService;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
import com.wa.finance.security.PhoneProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioService usuarioService;
    private final OutboxService outboxService;
    private final PhoneProtectionService phoneProtectionService;

    @Transactional
    public TransacaoResponseDTO registrarViaApi(TransacaoRequestDTO dto) {
        return TransacaoResponseDTO.from(processarTransacaoDaFila(dto));
    }

    @Transactional
    public Transacao processarTransacaoDaFila(TransacaoRequestDTO dto) {
        validarRequisicao(dto);

        if (dto.messageId() != null && !dto.messageId().isBlank()) {
            var transacaoExistente = transacaoRepository.findByExternalMessageId(dto.messageId());
            if (transacaoExistente.isPresent()) {
                log.info("Mensagem {} já processada; transação duplicada ignorada", dto.messageId());
                return transacaoExistente.get();
            }
        }

        Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(dto.telefone().trim());

        Categoria categoria = categoriaRepository
                .findByNomeIgnoreCaseAndUsuarioIdAndAtivaTrue(dto.categoriaNome().trim(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        BAD_REQUEST,
                        "A categoria informada não existe ou está desativada para o usuário"
                ));

        Transacao transacao = new Transacao();
        transacao.setExternalMessageId(dto.messageId());
        transacao.setValor(dto.valor());
        transacao.setDescricao(dto.descricao().trim());
        transacao.setCategoria(categoria);
        transacao.setUsuario(usuario);

        transacaoRepository.saveAndFlush(transacao);

        outboxService.adicionarConfirmacao(
                new RespostaUsuarioDTO(
                        usuarioService.obterTelefone(usuario),
                        String.format(
                                "✅ *Gasto registrado!*\n\n" +
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

    private void validarRequisicao(TransacaoRequestDTO dto) {
        if (dto == null) {
            throw requisicaoInvalida("Os dados da transação são obrigatórios");
        }
        if (dto.telefone() == null || dto.telefone().isBlank()) {
            throw requisicaoInvalida("O telefone do usuário é obrigatório");
        }
        if (dto.valor() == null || dto.valor().signum() <= 0) {
            throw requisicaoInvalida("O valor deve ser maior que zero");
        }
        if (dto.descricao() == null || dto.descricao().isBlank()) {
            throw requisicaoInvalida("A descrição é obrigatória");
        }
        if (dto.descricao().trim().length() > 255) {
            throw requisicaoInvalida("A descrição deve ter no máximo 255 caracteres");
        }
        if (dto.categoriaNome() == null || dto.categoriaNome().isBlank()) {
            throw requisicaoInvalida("O nome da categoria é obrigatório");
        }
        if (dto.categoriaNome().trim().length() > 50) {
            throw requisicaoInvalida("O nome da categoria deve ter no máximo 50 caracteres");
        }
    }

    private ResponseStatusException requisicaoInvalida(String mensagem) {
        return new ResponseStatusException(BAD_REQUEST, mensagem);
    }

    @Transactional
    public CancelamentoTransacaoDTO cancelar(Long transacaoId, String telefone) {
        Transacao transacao = transacaoRepository
                .findByIdAndUsuarioTelefoneHash(
                        transacaoId,
                        phoneProtectionService.lookupHash(telefone)
                )
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
