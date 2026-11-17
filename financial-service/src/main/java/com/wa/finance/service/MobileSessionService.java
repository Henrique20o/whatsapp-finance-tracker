package com.wa.finance.service;

import com.wa.finance.domain.SessaoMobile;
import com.wa.finance.dto.mobile.auth.TokenResponse;
import com.wa.finance.dto.mobile.SessaoMobileResponse;
import com.wa.finance.repository.SessaoMobileRepository;
import com.wa.finance.security.MobileSecurityProperties;
import com.wa.finance.security.RefreshTokenSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
public class MobileSessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MobileSessionService.class);
    private final SessaoMobileRepository repository;
    private final MobileAccessTokenService accessTokenService;
    private final MobileSecurityProperties properties;
    private final Clock clock;
    private final MobileSessionMetrics metrics;

    public MobileSessionService(
            SessaoMobileRepository repository,
            MobileAccessTokenService accessTokenService,
            MobileSecurityProperties properties,
            Clock clock,
            MobileSessionMetrics metrics
    ) {
        this.repository = repository;
        this.accessTokenService = accessTokenService;
        this.properties = properties;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public TokenResponse criar(Long usuarioId, String nomeDispositivo) {
        Instant now = clock.instant();
        UUID familyId = UUID.randomUUID();
        String refreshToken = RefreshTokenSupport.gerar();
        Instant inactivityExpiry = now.plus(properties.getRefreshTokenInactivity());
        Instant absoluteExpiry = now.plus(properties.getSessionAbsoluteTtl());
        repository.save(SessaoMobile.criar(
                UUID.randomUUID(), familyId, usuarioId, RefreshTokenSupport.hash(refreshToken), null,
                normalizarNome(nomeDispositivo), now, inactivityExpiry, absoluteExpiry));
        var access = accessTokenService.emit(usuarioId, familyId);
        return response(access, refreshToken, now, inactivityExpiry, absoluteExpiry);
    }

    @Transactional
    public TokenResponse renovar(String presentedToken) {
        Instant now = clock.instant();
        var found = repository.findByRefreshTokenHashForUpdate(RefreshTokenSupport.hash(presentedToken));
        if (found.isEmpty()) {
            metrics.refresh("invalid");
            return null;
        }
        SessaoMobile previous = found.get();
        List<SessaoMobile> family = repository.findFamiliaForUpdate(previous.getSessaoId());

        if (previous.getUsadoEm() != null) {
            family.stream().filter(s -> s.getRevogadoEm() == null)
                    .forEach(s -> s.revogar(now, "REFRESH_REUSE"));
            metrics.refresh("replay");
            LOGGER.warn("event=MOBILE_AUTH_REFRESH_REPLAY correlationId={}",
                    org.slf4j.MDC.get("correlationId"));
            return null;
        }
        if (previous.getRevogadoEm() != null || !previous.estaAtiva(now)) {
            metrics.refresh("invalid");
            return null;
        }

        boolean hasActiveHead = family.stream().anyMatch(s -> s.getUsadoEm() == null && s.getRevogadoEm() == null);
        if (!hasActiveHead) {
            metrics.refresh("invalid");
            return null;
        }
        previous.marcarUsada(now);
        repository.flush();
        String nextToken = RefreshTokenSupport.gerar();
        Instant inactivityExpiry = now.plus(properties.getRefreshTokenInactivity());
        if (inactivityExpiry.isAfter(previous.getExpiraAbsolutoEm())) {
            inactivityExpiry = previous.getExpiraAbsolutoEm();
        }
        SessaoMobile next = SessaoMobile.criar(
                UUID.randomUUID(), previous.getSessaoId(), previous.getUsuarioId(),
                RefreshTokenSupport.hash(nextToken), previous.getId(), previous.getNomeDispositivo(),
                now, inactivityExpiry, previous.getExpiraAbsolutoEm());
        repository.save(next);
        var access = accessTokenService.emit(previous.getUsuarioId(), previous.getSessaoId());
        metrics.refresh("success");
        LOGGER.info("event=MOBILE_AUTH_REFRESH_SUCCEEDED correlationId={}",
                org.slf4j.MDC.get("correlationId"));
        return response(access, nextToken, now, inactivityExpiry, previous.getExpiraAbsolutoEm());
    }

    @Transactional
    public void logout(String presentedToken) {
        Instant now = clock.instant();
        var found = repository.findByRefreshTokenHashForUpdate(RefreshTokenSupport.hash(presentedToken));
        if (found.isEmpty()) {
            metrics.logout("unknown");
            return;
        }
        SessaoMobile session = found.get();
        List<SessaoMobile> family = repository.findFamiliaForUpdate(session.getSessaoId());
        String reason = session.getUsadoEm() == null ? "LOGOUT" : "REFRESH_REUSE";
        family.stream().filter(s -> s.getRevogadoEm() == null).forEach(s -> s.revogar(now, reason));
        metrics.logout("revoked");
    }

    @Transactional(readOnly = true)
    public List<SessaoMobileResponse> listar(Long userId, UUID currentSessionId) {
        Map<UUID, List<SessaoMobile>> families = repository.findAllByUsuarioIdOrderByCriadoEmDesc(userId)
                .stream().collect(java.util.stream.Collectors.groupingBy(SessaoMobile::getSessaoId));
        Instant now = clock.instant();
        return families.entrySet().stream().map(entry -> {
            SessaoMobile latest = entry.getValue().stream()
                    .max(Comparator.comparing(SessaoMobile::getCriadoEm)).orElseThrow();
            Instant expiry = latest.getExpiraInatividadeEm().isBefore(latest.getExpiraAbsolutoEm())
                    ? latest.getExpiraInatividadeEm() : latest.getExpiraAbsolutoEm();
            boolean active = latest.estaAtiva(now);
            return new SessaoMobileResponse(entry.getKey(), latest.getNomeDispositivo(),
                    latest.getCriadoEm(), latest.getCriadoEm(), expiry, active,
                    entry.getKey().equals(currentSessionId));
        }).sorted(Comparator.comparing(SessaoMobileResponse::criadaEm).reversed()).toList();
    }

    @Transactional
    public boolean revogar(UUID familyId, Long userId) {
        List<SessaoMobile> family = repository.findFamiliaDoUsuarioForUpdate(familyId, userId);
        if (family.isEmpty()) return false;
        Instant now = clock.instant();
        family.stream().filter(s -> s.getRevogadoEm() == null).forEach(s -> s.revogar(now, "USER_LOGOUT"));
        return true;
    }

    @Transactional
    public void revogarTodas(Long userId) {
        Instant now = clock.instant();
        repository.findTodasDoUsuarioForUpdate(userId).stream()
                .filter(s -> s.getRevogadoEm() == null)
                .forEach(s -> s.revogar(now, "USER_LOGOUT_ALL"));
    }

    private TokenResponse response(
            MobileAccessTokenService.AccessToken access, String refresh, Instant now,
            Instant inactivityExpiry, Instant absoluteExpiry
    ) {
        long refreshSeconds = Math.max(1, Duration.between(now,
                inactivityExpiry.isBefore(absoluteExpiry) ? inactivityExpiry : absoluteExpiry).toSeconds());
        return new TokenResponse(access.value(), "Bearer", access.expiresInSeconds(), refresh, refreshSeconds);
    }

    private String normalizarNome(String value) {
        if (value == null) return null;
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return null;
        if (normalized.length() > 120) throw new IllegalArgumentException("Nome do dispositivo excede 120 caracteres");
        return normalized;
    }
}
