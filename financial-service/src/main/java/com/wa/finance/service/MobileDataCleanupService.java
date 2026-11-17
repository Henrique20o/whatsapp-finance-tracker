package com.wa.finance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Component
public class MobileDataCleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MobileDataCleanupService.class);
    private static final Duration RETENTION_AFTER_EXPIRY = Duration.ofDays(30);
    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public MobileDataCleanupService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 3_600_000L, initialDelay = 3_600_000L)
    public void cleanup() {
        var cutoff = Timestamp.from(clock.instant().minus(RETENTION_AFTER_EXPIRY));
        int challenges = jdbc.update("""
                DELETE FROM tb_desafio_autenticacao WHERE id IN (
                    SELECT id FROM tb_desafio_autenticacao
                     WHERE expira_em < ? ORDER BY expira_em LIMIT ?)
                """, cutoff, BATCH_SIZE);
        int idempotency = jdbc.update("""
                DELETE FROM tb_idempotencia_mobile WHERE id IN (
                    SELECT id FROM tb_idempotencia_mobile
                     WHERE criada_em < ? ORDER BY criada_em LIMIT ?)
                """, cutoff, BATCH_SIZE);
        int sessions = cleanupExpiredFamilies(cutoff);
        if (challenges + idempotency + sessions > 0) {
            LOGGER.info("event=MOBILE_DATA_CLEANUP challenges={} idempotency={} sessionRows={}",
                    challenges, idempotency, sessions);
        }
    }

    private int cleanupExpiredFamilies(Timestamp cutoff) {
        var families = jdbc.query("""
                SELECT sessao_id FROM tb_sessao_mobile
                 GROUP BY sessao_id
                HAVING bool_and(expira_absoluto_em < ?)
                 LIMIT ?
                """, (rs, row) -> rs.getObject(1, UUID.class), cutoff, BATCH_SIZE);
        int deleted = 0;
        for (UUID family : families) {
            int count;
            do {
                count = jdbc.update("""
                        DELETE FROM tb_sessao_mobile parent
                         WHERE parent.id = (
                            SELECT candidate.id FROM tb_sessao_mobile candidate
                             WHERE candidate.sessao_id = ?
                               AND NOT EXISTS (
                                   SELECT 1 FROM tb_sessao_mobile child
                                    WHERE child.token_anterior_id = candidate.id)
                             LIMIT 1)
                        """, family);
                deleted += count;
            } while (count > 0);
        }
        return deleted;
    }
}
