package com.wa.finance.service;

import com.wa.finance.repository.DesafioAutenticacaoLockRepository;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.MobileAuthRateLimitProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class MobileAuthRateLimitService {

    private static final Duration HOUR = Duration.ofHours(1);
    private static final Duration DAY = Duration.ofHours(24);

    private final DesafioAutenticacaoRepository repository;
    private final DesafioAutenticacaoLockRepository lockRepository;
    private final MobileAuthRateLimitProperties properties;
    private final MobileAuthRateLimitMetrics metrics;
    private final Clock clock;

    public MobileAuthRateLimitService(
            DesafioAutenticacaoRepository repository,
            DesafioAutenticacaoLockRepository lockRepository,
            MobileAuthRateLimitProperties properties,
            MobileAuthRateLimitMetrics metrics,
            Clock clock
    ) {
        this.repository = repository;
        this.lockRepository = lockRepository;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    public RateLimitDecision evaluate(String phoneHash, String ipHash) {
        Objects.requireNonNull(phoneHash);
        Objects.requireNonNull(ipHash);

        lockRepository.adquirir(lockId(RateLimitScope.PHONE, phoneHash));
        lockRepository.adquirir(lockId(RateLimitScope.IP, ipHash));
        Instant now = clock.instant();

        List<Instant> phoneHistory = repository.findCriacoesRecentesPorTelefone(
                phoneHash,
                now.minus(DAY),
                PageRequest.of(0, properties.getPhonePerDay())
        );
        List<Instant> ipHistory = repository.findCriacoesRecentesPorIp(
                ipHash,
                now.minus(HOUR),
                PageRequest.of(0, properties.getIpPerHour())
        );

        Candidate longest = null;
        Instant resendAllowedAt = repository.findUltimoReenvioPermitidoPorTelefone(phoneHash)
                .orElse(null);
        longest = later(longest, candidate(
                resendAllowedAt, now, RateLimitScope.PHONE, RateLimitRule.RESEND));

        List<Instant> phoneHour = phoneHistory.stream()
                .filter(createdAt -> createdAt.isAfter(now.minus(HOUR)))
                .toList();
        if (phoneHour.size() >= properties.getPhonePerHour()) {
            Instant boundary = phoneHour.get(properties.getPhonePerHour() - 1).plus(HOUR);
            longest = later(longest, candidate(
                    boundary, now, RateLimitScope.PHONE, RateLimitRule.HOURLY));
        }

        if (phoneHistory.size() >= properties.getPhonePerDay()) {
            Instant boundary = phoneHistory.get(properties.getPhonePerDay() - 1).plus(DAY);
            longest = later(longest, candidate(
                    boundary, now, RateLimitScope.PHONE, RateLimitRule.DAILY));
        }

        if (ipHistory.size() >= properties.getIpPerHour()) {
            Instant boundary = ipHistory.get(properties.getIpPerHour() - 1).plus(HOUR);
            longest = later(longest, candidate(
                    boundary, now, RateLimitScope.IP, RateLimitRule.HOURLY));
        }

        if (longest == null) {
            return RateLimitDecision.permit(now);
        }

        RateLimitDecision decision = RateLimitDecision.blocked(
                now,
                longest.blockedUntil(),
                secondsRoundedUp(Duration.between(now, longest.blockedUntil())),
                longest.scope(),
                longest.rule()
        );
        metrics.recordRejection(decision);
        return decision;
    }

    static long lockId(RateLimitScope scope, String subjectHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = (scope.name() + ":" + subjectHash)
                    .getBytes(StandardCharsets.US_ASCII);
            return ByteBuffer.wrap(digest.digest(value), 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private Candidate candidate(
            Instant blockedUntil,
            Instant now,
            RateLimitScope scope,
            RateLimitRule rule
    ) {
        if (blockedUntil == null || !blockedUntil.isAfter(now)) {
            return null;
        }
        return new Candidate(blockedUntil, scope, rule);
    }

    private Candidate later(Candidate current, Candidate candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.blockedUntil().isAfter(current.blockedUntil())) {
            return candidate;
        }
        return current;
    }

    private long secondsRoundedUp(Duration duration) {
        long seconds = duration.getSeconds();
        if (duration.getNano() > 0) {
            seconds = Math.addExact(seconds, 1);
        }
        return Math.max(1, seconds);
    }

    private record Candidate(
            Instant blockedUntil,
            RateLimitScope scope,
            RateLimitRule rule
    ) {
    }
}
