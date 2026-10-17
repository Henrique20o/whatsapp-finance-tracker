package com.wa.finance.service;

import com.wa.finance.security.VerificationCodeProperties;
import com.wa.finance.outbox.OutboxService;
import com.wa.finance.security.mobile.delivery.MobileAuthDeliveryCipher;
import com.wa.finance.security.mobile.delivery.MobileAuthDeliveryEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitacaoCodigoServiceTest {

    @Test
    void deveCriarDesafioUmaVezEMapearSomenteResultadoSeguro() {
        TestDesafioAutenticacaoService desafioService = new TestDesafioAutenticacaoService();
        VerificationCodeProperties properties = properties(Duration.ofMinutes(5), Duration.ofMinutes(1));
        MobileAuthDeliveryCipher cipher = mock(MobileAuthDeliveryCipher.class);
        OutboxService outboxService = mock(OutboxService.class);
        SolicitacaoCodigoService service = new SolicitacaoCodigoService(
                desafioService, properties, cipher, outboxService);
        UUID challengeId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        MobileAuthDeliveryEnvelope envelope = envelope(challengeId, correlationId);
        when(cipher.encrypt(any(), any(), any(), any(), anyString(), anyString()))
                .thenReturn(envelope);

        desafioService.resultado = new DesafioCriado(
                        challengeId,
                        "5531999998888",
                        "004291",
                        Instant.parse("2026-10-14T20:05:00Z"),
                        Instant.parse("2026-10-14T20:01:00Z")
                );

        SolicitacaoCodigoCriada resultado = service.solicitar(
                "5531999998888", "192.0.2.10", correlationId.toString());

        assertThat(resultado.challengeId()).isEqualTo(challengeId);
        assertThat(resultado.expiraEmSegundos()).isEqualTo(300);
        assertThat(resultado.reenviarEmSegundos()).isEqualTo(60);
        assertThat(resultado.toString())
                .doesNotContain("5531999998888")
                .doesNotContain("004291");
        assertThat(desafioService.telefoneRecebido).isEqualTo("5531999998888");
        assertThat(desafioService.ipRecebido).isEqualTo("192.0.2.10");
        assertThat(desafioService.chamadas).isEqualTo(1);
        verify(cipher).encrypt(
                any(UUID.class),
                eq(challengeId),
                eq(correlationId),
                eq(Instant.parse("2026-10-14T20:05:00Z")),
                eq("5531999998888"),
                eq("004291")
        );
        verify(outboxService).adicionarCodigoAutenticacao(envelope);
    }

    @Test
    void deveArredondarDuracoesPositivasParaCima() {
        TestDesafioAutenticacaoService desafioService = new TestDesafioAutenticacaoService();
        MobileAuthDeliveryCipher cipher = mock(MobileAuthDeliveryCipher.class);
        SolicitacaoCodigoService service = new SolicitacaoCodigoService(
                desafioService,
                properties(Duration.ofMillis(1500), Duration.ofMillis(500)),
                cipher,
                mock(OutboxService.class)
        );
        UUID challengeId = UUID.randomUUID();
        desafioService.resultado = new DesafioCriado(
                        challengeId,
                        "5531999998888",
                        "482913",
                        Instant.now().plusMillis(1500),
                        Instant.now().plusMillis(500)
                );

        UUID correlationId = UUID.randomUUID();
        when(cipher.encrypt(any(), any(), any(), any(), anyString(), anyString()))
                .thenReturn(envelope(challengeId, correlationId));

        SolicitacaoCodigoCriada resultado = service.solicitar(
                "5531999998888", "192.0.2.10", correlationId.toString());

        assertThat(resultado.expiraEmSegundos()).isEqualTo(2);
        assertThat(resultado.reenviarEmSegundos()).isEqualTo(1);
    }

    @Test
    void devePropagarFalhaDaCriacao() {
        TestDesafioAutenticacaoService desafioService = new TestDesafioAutenticacaoService();
        SolicitacaoCodigoService service = new SolicitacaoCodigoService(
                desafioService,
                properties(Duration.ofMinutes(5), Duration.ofMinutes(1)),
                mock(MobileAuthDeliveryCipher.class),
                mock(OutboxService.class)
        );
        desafioService.falha = new IllegalStateException("falha técnica");

        assertThatThrownBy(() -> service.solicitar(
                "5531999998888", "192.0.2.10", UUID.randomUUID().toString()))
                .isInstanceOf(IllegalStateException.class);
    }

    private MobileAuthDeliveryEnvelope envelope(UUID challengeId, UUID correlationId) {
        return new MobileAuthDeliveryEnvelope(
                1,
                UUID.randomUUID(),
                challengeId,
                correlationId,
                Instant.parse("2026-10-14T20:05:00Z"),
                "v1",
                "iv",
                "ciphertext"
        );
    }

    private VerificationCodeProperties properties(Duration ttl, Duration resend) {
        VerificationCodeProperties properties = new VerificationCodeProperties();
        properties.setTtl(ttl);
        properties.setResendInterval(resend);
        return properties;
    }

    private static final class TestDesafioAutenticacaoService extends DesafioAutenticacaoService {

        private DesafioCriado resultado;
        private RuntimeException falha;
        private String telefoneRecebido;
        private String ipRecebido;
        private int chamadas;

        private TestDesafioAutenticacaoService() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public DesafioCriado criar(String telefone, String enderecoIpCanonico) {
            chamadas++;
            telefoneRecebido = telefone;
            ipRecebido = enderecoIpCanonico;
            if (falha != null) {
                throw falha;
            }
            return resultado;
        }
    }
}
