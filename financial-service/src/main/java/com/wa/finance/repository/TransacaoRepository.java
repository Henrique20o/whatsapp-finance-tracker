package com.wa.finance.repository;

import com.wa.finance.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    Optional<Transacao> findByExternalMessageId(String externalMessageId);
}
