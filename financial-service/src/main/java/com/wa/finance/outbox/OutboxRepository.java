package com.wa.finance.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    @Query(value = """
            SELECT *
              FROM tb_outbox
             WHERE status = 'PENDENTE'
               AND proxima_tentativa_em <= :limite
             ORDER BY id
             LIMIT 1
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxMessage> findNextPendingForUpdate(@Param("limite") LocalDateTime limite);
}
