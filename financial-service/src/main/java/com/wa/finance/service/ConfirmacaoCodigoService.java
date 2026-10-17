package com.wa.finance.service;

import com.wa.finance.domain.DesafioAutenticacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.PhoneProtectionService;
import com.wa.finance.security.VerificationCodeHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class ConfirmacaoCodigoService {

    private final DesafioAutenticacaoRepository desafioRepository;
    private final PhoneProtectionService phoneProtectionService;
    private final VerificationCodeHasher codeHasher;
    private final UsuarioService usuarioService;
    private final Clock clock;

    public ConfirmacaoCodigoService(
            DesafioAutenticacaoRepository desafioRepository,
            PhoneProtectionService phoneProtectionService,
            VerificationCodeHasher codeHasher,
            UsuarioService usuarioService,
            Clock clock
    ) {
        this.desafioRepository = desafioRepository;
        this.phoneProtectionService = phoneProtectionService;
        this.codeHasher = codeHasher;
        this.usuarioService = usuarioService;
        this.clock = clock;
    }

    @Transactional
    public ConfirmacaoCodigoResultado confirmar(
            UUID challengeId,
            String codigo,
            String nomeDispositivo,
            String correlationId
    ) {
        Instant agora = clock.instant();
        var encontrado = desafioRepository.findByIdForUpdate(challengeId);
        if (encontrado.isEmpty()) {
            registrarRejeicao(challengeId, correlationId, "NOT_FOUND");
            return ConfirmacaoCodigoResultado.rejeitado();
        }

        DesafioAutenticacao desafio = encontrado.get();
        if (!desafio.estaDisponivel(agora)) {
            registrarRejeicao(challengeId, correlationId, categoriaIndisponibilidade(desafio, agora));
            return ConfirmacaoCodigoResultado.rejeitado();
        }

        String telefone = phoneProtectionService.decrypt(desafio.getTelefoneCriptografado());
        if (!phoneProtectionService.matchesLookupHash(desafio.getTelefoneHash(), telefone)) {
            throw new IllegalStateException("Integridade do telefone do desafio inválida");
        }

        if (!codeHasher.matches(desafio.getCodigoHmac(), desafio.getId(), telefone, codigo)) {
            desafio.registrarTentativaIncorreta(agora);
            registrarRejeicao(challengeId, correlationId, "INVALID_CODE");
            return ConfirmacaoCodigoResultado.rejeitado();
        }

        Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(telefone);
        desafio.consumir(agora);
        String nomeNormalizado = nomeDispositivo == null ? null : nomeDispositivo.trim();
        log.info(
                "event=MOBILE_AUTH_VERIFY_CONFIRMED challengeId={} correlationId={} userId={}",
                challengeId, correlationId, usuario.getId());
        return ConfirmacaoCodigoResultado.confirmado(
                new CodigoConfirmado(usuario.getId(), nomeNormalizado));
    }

    private void registrarRejeicao(UUID challengeId, String correlationId, String categoria) {
        log.info(
                "event=MOBILE_AUTH_VERIFY_REJECTED challengeId={} correlationId={} category={}",
                challengeId, correlationId, categoria);
    }

    private String categoriaIndisponibilidade(DesafioAutenticacao desafio, Instant agora) {
        if (desafio.getConsumidoEm() != null) {
            return "CONSUMED";
        }
        if (desafio.getInvalidadoEm() != null) {
            return "INVALIDATED";
        }
        if (desafio.estaExpirado(agora)) {
            return "EXPIRED";
        }
        return "ATTEMPTS_EXHAUSTED";
    }
}
