package com.wa.finance.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    List<OutboxMessage> findByStatusAndProximaTentativaEmLessThanEqualOrderById(
            OutboxStatus status,
            LocalDateTime limite,
            Pageable pageable
    );
}
