package com.wa.finance.repository;

import com.wa.finance.domain.DesafioAutenticacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DesafioAutenticacaoRepository extends JpaRepository<DesafioAutenticacao, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DesafioAutenticacao d where d.id = :id")
    java.util.Optional<DesafioAutenticacao> findByIdForUpdate(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update DesafioAutenticacao d
               set d.invalidadoEm = :agora
             where d.telefoneHash = :telefoneHash
               and d.consumidoEm is null
               and d.invalidadoEm is null
            """)
    int invalidarNaoFinalizadosPorTelefone(
            @Param("telefoneHash") String telefoneHash,
            @Param("agora") Instant agora
    );

    List<DesafioAutenticacao> findAllByTelefoneHashOrderByCriadoEmAsc(String telefoneHash);
}
