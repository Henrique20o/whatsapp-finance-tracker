package com.wa.finance.service;

import com.wa.finance.security.VerificationCodeProperties;
import com.wa.finance.outbox.OutboxService;
import com.wa.finance.security.mobile.delivery.MobileAuthDeliveryCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class SolicitacaoCodigoService {

    private final DesafioAutenticacaoService desafioAutenticacaoService;
    private final VerificationCodeProperties properties;
    private final MobileAuthDeliveryCipher deliveryCipher;
    private final OutboxService outboxService;

    public SolicitacaoCodigoService(
            DesafioAutenticacaoService desafioAutenticacaoService,
            VerificationCodeProperties properties,
            MobileAuthDeliveryCipher deliveryCipher,
            OutboxService outboxService
    ) {
        this.desafioAutenticacaoService = desafioAutenticacaoService;
        this.properties = properties;
        this.deliveryCipher = deliveryCipher;
        this.outboxService = outboxService;
    }

    @Transactional
    public SolicitacaoCodigoCriada solicitar(String telefone, String ipCanonico, String correlationId) {
        DesafioCriado desafio = desafioAutenticacaoService.criar(telefone, ipCanonico);
        var envelope = deliveryCipher.encrypt(
                UUID.randomUUID(),
                desafio.getChallengeId(),
                UUID.fromString(correlationId),
                desafio.getExpiraEm(),
                desafio.getTelefoneParaEnvio(),
                desafio.getCodigoParaEnvio()
        );
        outboxService.adicionarCodigoAutenticacao(envelope);
        return new SolicitacaoCodigoCriada(
                desafio.getChallengeId(),
                segundosArredondadosParaCima(properties.getTtl()),
                segundosArredondadosParaCima(properties.getResendInterval())
        );
    }

    private long segundosArredondadosParaCima(Duration duration) {
        long segundosCompletos = duration.toSeconds();
        if (duration.minusSeconds(segundosCompletos).isZero()) {
            return segundosCompletos;
        }
        return Math.addExact(segundosCompletos, 1);
    }
}
