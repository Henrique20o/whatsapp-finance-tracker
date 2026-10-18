package com.wa.finance.service;

import com.wa.finance.repository.DesafioAutenticacaoLockRepository;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.MobileAuthRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileAuthRateLimitServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T15:00:00Z");
    private static final String PHONE_HASH = "a".repeat(64);
    private static final String IP_HASH = "b".repeat(64);

    private DesafioAutenticacaoRepository repository;
    private DesafioAutenticacaoLockRepository lockRepository;
    private MobileAuthRateLimitMetrics metrics;
    private Clock clock;
    private MobileAuthRateLimitService service;

    @BeforeEach
    void configure() {
        repository = mock(DesafioAutenticacaoRepository.class);
        lockRepository = mock(DesafioAutenticacaoLockRepository.class);
        metrics = mock(MobileAuthRateLimitMetrics.class);
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        MobileAuthRateLimitProperties properties = new MobileAuthRateLimitProperties();
        service = new MobileAuthRateLimitService(
                repository,
                lockRepository,
                properties,
                metrics,
                clock
        );

        when(repository.findUltimoReenvioPermitidoPorTelefone(PHONE_HASH))
                .thenReturn(Optional.empty());
        when(repository.findCriacoesRecentesPorTelefone(
                eq(PHONE_HASH), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(repository.findCriacoesRecentesPorIp(
                eq(IP_HASH), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
    }

    @Test
    void deveCapturarInstanteSomenteDepoisDeAdquirirLocks() {
        RateLimitDecision decision = service.evaluate(PHONE_HASH, IP_HASH);

        assertThat(decision.allowed()).isTrue();
        InOrder order = inOrder(lockRepository, clock);
        order.verify(lockRepository).adquirir(
                MobileAuthRateLimitService.lockId(RateLimitScope.PHONE, PHONE_HASH));
        order.verify(lockRepository).adquirir(
                MobileAuthRateLimitService.lockId(RateLimitScope.IP, IP_HASH));
        order.verify(clock).instant();
        assertThat(decision.evaluatedAt()).isEqualTo(NOW);
        verify(metrics, never()).recordRejection(any());
    }

    @Test
    void deveBloquearReenvioEArredondarParaCima() {
        when(repository.findUltimoReenvioPermitidoPorTelefone(PHONE_HASH))
                .thenReturn(Optional.of(NOW.plusMillis(60_001)));

        RateLimitDecision decision = service.evaluate(PHONE_HASH, IP_HASH);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.scope()).isEqualTo(RateLimitScope.PHONE);
        assertThat(decision.rule()).isEqualTo(RateLimitRule.RESEND);
        assertThat(decision.retryAfterSeconds()).isEqualTo(61);
        verify(metrics).recordRejection(decision);
    }

    @Test
    void devePermitirExatamenteNoInstanteDoReenvio() {
        when(repository.findUltimoReenvioPermitidoPorTelefone(PHONE_HASH))
                .thenReturn(Optional.of(NOW));

        assertThat(service.evaluate(PHONE_HASH, IP_HASH).allowed()).isTrue();
    }

    @Test
    void deveBloquearPeloQuintoRegistroDaHora() {
        when(repository.findCriacoesRecentesPorTelefone(
                eq(PHONE_HASH), any(Instant.class), any(Pageable.class)))
                .thenReturn(descendingHistory(5, 10));

        RateLimitDecision decision = service.evaluate(PHONE_HASH, IP_HASH);

        assertThat(decision.rule()).isEqualTo(RateLimitRule.HOURLY);
        assertThat(decision.scope()).isEqualTo(RateLimitScope.PHONE);
        assertThat(decision.retryAfterSeconds()).isEqualTo(3_550);
    }

    @Test
    void deveBloquearPeloDecimoRegistroDoDia() {
        List<Instant> history = new ArrayList<>();
        for (int index = 1; index <= 10; index++) {
            history.add(NOW.minusSeconds(index * 3_600L));
        }
        when(repository.findCriacoesRecentesPorTelefone(
                eq(PHONE_HASH), any(Instant.class), any(Pageable.class)))
                .thenReturn(history);

        RateLimitDecision decision = service.evaluate(PHONE_HASH, IP_HASH);

        assertThat(decision.rule()).isEqualTo(RateLimitRule.DAILY);
        assertThat(decision.scope()).isEqualTo(RateLimitScope.PHONE);
        assertThat(decision.retryAfterSeconds()).isEqualTo(50_400);
    }

    @Test
    void deveBloquearPeloTrigesimoRegistroDoIp() {
        when(repository.findCriacoesRecentesPorIp(
                eq(IP_HASH), any(Instant.class), any(Pageable.class)))
                .thenReturn(descendingHistory(30, 30));

        RateLimitDecision decision = service.evaluate(PHONE_HASH, IP_HASH);

        assertThat(decision.rule()).isEqualTo(RateLimitRule.HOURLY);
        assertThat(decision.scope()).isEqualTo(RateLimitScope.IP);
        assertThat(decision.retryAfterSeconds()).isEqualTo(2_700);
    }

    @Test
    void deveEscolherMaiorTempoEntreRegrasVioladas() {
        when(repository.findUltimoReenvioPermitidoPorTelefone(PHONE_HASH))
                .thenReturn(Optional.of(NOW.plusSeconds(45)));
        when(repository.findCriacoesRecentesPorIp(
                eq(IP_HASH), any(Instant.class), any(Pageable.class)))
                .thenReturn(descendingHistory(30, 1));

        RateLimitDecision decision = service.evaluate(PHONE_HASH, IP_HASH);

        assertThat(decision.scope()).isEqualTo(RateLimitScope.IP);
        assertThat(decision.rule()).isEqualTo(RateLimitRule.HOURLY);
        assertThat(decision.retryAfterSeconds()).isEqualTo(3_570);
    }

    @Test
    void deveSepararDominioDosLocks() {
        assertThat(MobileAuthRateLimitService.lockId(RateLimitScope.PHONE, PHONE_HASH))
                .isNotEqualTo(MobileAuthRateLimitService.lockId(RateLimitScope.IP, PHONE_HASH));
    }

    private List<Instant> descendingHistory(int size, long spacingSeconds) {
        List<Instant> history = new ArrayList<>();
        for (int index = 1; index <= size; index++) {
            history.add(NOW.minusSeconds(index * spacingSeconds));
        }
        return history;
    }
}
