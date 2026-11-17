package com.wa.finance.service;

import com.wa.finance.domain.SessaoMobile;
import com.wa.finance.repository.SessaoMobileRepository;
import com.wa.finance.security.MobileSecurityProperties;
import com.wa.finance.security.RefreshTokenSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MobileSessionServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");

    @Mock private SessaoMobileRepository repository;
    @Mock private MobileAccessTokenService accessTokens;
    @Mock private MobileSessionMetrics metrics;
    private MobileSecurityProperties properties;
    private MobileSessionService service;

    @BeforeEach
    void configure() {
        properties = new MobileSecurityProperties();
        properties.setRefreshTokenInactivity(Duration.ofDays(30));
        properties.setSessionAbsoluteTtl(Duration.ofDays(90));
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        service = new MobileSessionService(repository, accessTokens, properties,
                Clock.fixed(NOW, ZoneOffset.UTC), metrics);
        lenient().when(accessTokens.emit(any(), any())).thenReturn(
                new MobileAccessTokenService.AccessToken("jwt", 900, NOW.plusSeconds(900)));
    }

    @Test
    void criaFamiliaComHashDoRefreshEExpiracoesConfiguradas() {
        var response = service.criar(42L, "  Pixel   10 ");
        ArgumentCaptor<SessaoMobile> saved = ArgumentCaptor.forClass(SessaoMobile.class);
        verify(repository).save(saved.capture());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessTokenExpiraEmSegundos()).isEqualTo(900);
        assertThat(response.refreshTokenExpiraEmSegundos()).isEqualTo(30L * 24 * 60 * 60);
        assertThat(saved.getValue().getUsuarioId()).isEqualTo(42L);
        assertThat(saved.getValue().getNomeDispositivo()).isEqualTo("Pixel 10");
        assertThat(saved.getValue().getRefreshTokenHash()).isEqualTo(RefreshTokenSupport.hash(response.refreshToken()));
        assertThat(saved.getValue().getRefreshTokenHash()).isNotEqualTo(response.refreshToken());
        assertThat(saved.getValue().getExpiraAbsolutoEm()).isEqualTo(NOW.plus(Duration.ofDays(90)));
    }

    @Test
    void giraRefreshEGravaSucessorNaMesmaFamilia() {
        UUID familyId = UUID.randomUUID();
        String oldToken = RefreshTokenSupport.gerar();
        SessaoMobile previous = SessaoMobile.criar(UUID.randomUUID(), familyId, 42L,
                RefreshTokenSupport.hash(oldToken), null, "tablet", NOW.minus(Duration.ofDays(2)),
                NOW.plus(Duration.ofDays(28)), NOW.plus(Duration.ofDays(88)));
        when(repository.findByRefreshTokenHashForUpdate(RefreshTokenSupport.hash(oldToken)))
                .thenReturn(Optional.of(previous));
        when(repository.findFamiliaForUpdate(familyId)).thenReturn(List.of(previous));

        var response = service.renovar(oldToken);
        ArgumentCaptor<SessaoMobile> saved = ArgumentCaptor.forClass(SessaoMobile.class);
        verify(repository).save(saved.capture());

        assertThat(response).isNotNull();
        assertThat(previous.getUsadoEm()).isEqualTo(NOW);
        assertThat(saved.getValue().getSessaoId()).isEqualTo(familyId);
        assertThat(saved.getValue().getTokenAnteriorId()).isEqualTo(previous.getId());
        assertThat(saved.getValue().getRefreshTokenHash()).isEqualTo(RefreshTokenSupport.hash(response.refreshToken()));
        assertThat(saved.getValue().getExpiraInatividadeEm()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void replayRevogaFamiliaAntesDeRetornarFalha() {
        UUID familyId = UUID.randomUUID();
        SessaoMobile used = SessaoMobile.criar(UUID.randomUUID(), familyId, 42L,
                RefreshTokenSupport.hash("used"), null, null, NOW.minusSeconds(20),
                NOW.plus(Duration.ofDays(10)), NOW.plus(Duration.ofDays(20)));
        used.marcarUsada(NOW.minusSeconds(10));
        SessaoMobile active = SessaoMobile.criar(UUID.randomUUID(), familyId, 42L,
                RefreshTokenSupport.hash("current"), used.getId(), null, NOW.minusSeconds(5),
                NOW.plus(Duration.ofDays(10)), NOW.plus(Duration.ofDays(20)));
        when(repository.findByRefreshTokenHashForUpdate(RefreshTokenSupport.hash("old-token")))
                .thenReturn(Optional.of(used));
        when(repository.findFamiliaForUpdate(familyId)).thenReturn(List.of(active, used));

        assertThat(service.renovar("old-token")).isNull();
        assertThat(active.getRevogadoEm()).isEqualTo(NOW);
        assertThat(active.getMotivoRevogacao()).isEqualTo("REFRESH_REUSE");
        verify(metrics).refresh("replay");
    }
}
