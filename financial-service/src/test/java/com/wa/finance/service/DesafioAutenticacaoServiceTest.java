package com.wa.finance.service;

import com.wa.finance.domain.DesafioAutenticacao;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.ClientIpProtectionService;
import com.wa.finance.security.PhoneProtectionService;
import com.wa.finance.security.VerificationCodeGenerator;
import com.wa.finance.security.VerificationCodeHasher;
import com.wa.finance.security.VerificationCodeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesafioAutenticacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-14T17:00:00Z");

    private PhoneProtectionService phoneProtectionService;
    private VerificationCodeGenerator codeGenerator;
    private VerificationCodeHasher codeHasher;
    private ClientIpProtectionService ipProtectionService;
    private DesafioAutenticacaoRepository repository;
    private MobileAuthRateLimitService rateLimitService;
    private DesafioAutenticacaoService service;

    @BeforeEach
    void configurar() {
        phoneProtectionService = mock(PhoneProtectionService.class);
        codeGenerator = mock(VerificationCodeGenerator.class);
        codeHasher = mock(VerificationCodeHasher.class);
        ipProtectionService = mock(ClientIpProtectionService.class);
        repository = mock(DesafioAutenticacaoRepository.class);
        rateLimitService = mock(MobileAuthRateLimitService.class);
        when(rateLimitService.evaluate(any(), any(), any()))
                .thenReturn(RateLimitDecision.permit());

        VerificationCodeProperties properties = new VerificationCodeProperties();
        properties.setHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        properties.setTtl(Duration.ofMinutes(5));
        properties.setResendInterval(Duration.ofMinutes(1));

        service = new DesafioAutenticacaoService(
                phoneProtectionService,
                codeGenerator,
                codeHasher,
                ipProtectionService,
                repository,
                rateLimitService,
                properties,
                Clock.fixed(AGORA, ZoneOffset.UTC)
        );
    }

    @Test
    void deveProtegerPersistirEDevolverSomenteDadosParaEnvio() {
        String entrada = "+55 (31) 99999-8888";
        String normalizado = "5531999998888";
        String telefoneHash = "a".repeat(64);
        String codigoHmac = "b".repeat(64);
        String ipHash = "c".repeat(64);

        when(phoneProtectionService.normalize(entrada)).thenReturn(normalizado);
        when(phoneProtectionService.lookupHash(normalizado)).thenReturn(telefoneHash);
        when(phoneProtectionService.encrypt(normalizado)).thenReturn("telefone-protegido");
        when(codeGenerator.generate()).thenReturn("004291");
        when(codeHasher.hash(any(), org.mockito.ArgumentMatchers.eq(normalizado), org.mockito.ArgumentMatchers.eq("004291")))
                .thenReturn(codigoHmac);
        when(ipProtectionService.hash("192.0.2.10")).thenReturn(ipHash);

        DesafioCriado resultado = service.criar(entrada, "192.0.2.10");

        ArgumentCaptor<DesafioAutenticacao> captor = ArgumentCaptor.forClass(DesafioAutenticacao.class);
        verify(repository).save(captor.capture());
        DesafioAutenticacao persistido = captor.getValue();

        assertThat(persistido.getTelefoneCriptografado()).isEqualTo("telefone-protegido");
        assertThat(persistido.getTelefoneHash()).isEqualTo(telefoneHash);
        assertThat(persistido.getCodigoHmac()).isEqualTo(codigoHmac).isNotEqualTo("004291");
        assertThat(persistido.getIpHash()).isEqualTo(ipHash).isNotEqualTo("192.0.2.10");
        assertThat(persistido.getCriadoEm()).isEqualTo(AGORA);
        assertThat(persistido.getExpiraEm()).isEqualTo(AGORA.plusSeconds(300));
        assertThat(persistido.getReenvioPermitidoEm()).isEqualTo(AGORA.plusSeconds(60));

        assertThat(resultado.getChallengeId()).isEqualTo(persistido.getId());
        assertThat(resultado.getTelefoneParaEnvio()).isEqualTo(normalizado);
        assertThat(resultado.getCodigoParaEnvio()).isEqualTo("004291");
        assertThat(resultado.toString()).doesNotContain(normalizado).doesNotContain("004291");
    }

    @Test
    void deveAvaliarLimitesAntesDeInvalidarESalvar() {
        String telefoneHash = "0123456789abcdef" + "a".repeat(48);
        when(phoneProtectionService.normalize(any())).thenReturn("5531999998888");
        when(phoneProtectionService.lookupHash(any())).thenReturn(telefoneHash);
        when(phoneProtectionService.encrypt(any())).thenReturn("telefone-protegido");
        when(codeGenerator.generate()).thenReturn("482913");
        when(codeHasher.hash(any(), any(), any())).thenReturn("b".repeat(64));
        when(ipProtectionService.hash(any())).thenReturn("c".repeat(64));

        service.criar("5531999998888", "192.0.2.10");

        InOrder order = inOrder(rateLimitService, repository);
        order.verify(rateLimitService).evaluate(telefoneHash, "c".repeat(64), AGORA);
        order.verify(repository).invalidarNaoFinalizadosPorTelefone(telefoneHash, AGORA);
        order.verify(repository).save(any(DesafioAutenticacao.class));
    }

    @Test
    void naoDeveInvalidarNemGerarCodigoQuandoLimiteForExcedido() {
        String telefoneHash = "a".repeat(64);
        String ipHash = "b".repeat(64);
        when(phoneProtectionService.normalize(any())).thenReturn("5531999998888");
        when(phoneProtectionService.lookupHash(any())).thenReturn(telefoneHash);
        when(ipProtectionService.hash(any())).thenReturn(ipHash);
        when(rateLimitService.evaluate(telefoneHash, ipHash, AGORA))
                .thenReturn(RateLimitDecision.blocked(
                        AGORA.plusSeconds(60),
                        60,
                        RateLimitScope.PHONE,
                        RateLimitRule.RESEND
                ));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.criar("5531999998888", "192.0.2.10"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(exception -> assertThat(
                        ((RateLimitExceededException) exception).getRetryAfterSeconds())
                        .isEqualTo(60));

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .invalidarNaoFinalizadosPorTelefone(any(), any());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(codeGenerator, codeHasher);
    }
}
