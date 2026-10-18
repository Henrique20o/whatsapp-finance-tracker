package com.wa.finance.repository;

import com.wa.finance.domain.DesafioAutenticacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    @Query("""
            select max(d.reenvioPermitidoEm)
              from DesafioAutenticacao d
             where d.telefoneHash = :telefoneHash
            """)
    Optional<Instant> findUltimoReenvioPermitidoPorTelefone(
            @Param("telefoneHash") String telefoneHash
    );

    @Query("""
            select d.criadoEm
              from DesafioAutenticacao d
             where d.telefoneHash = :telefoneHash
               and d.criadoEm > :desde
             order by d.criadoEm desc
            """)
    List<Instant> findCriacoesRecentesPorTelefone(
            @Param("telefoneHash") String telefoneHash,
            @Param("desde") Instant desde,
            Pageable pageable
    );

    @Query("""
            select d.criadoEm
              from DesafioAutenticacao d
             where d.ipHash = :ipHash
               and d.criadoEm > :desde
             order by d.criadoEm desc
            """)
    List<Instant> findCriacoesRecentesPorIp(
            @Param("ipHash") String ipHash,
            @Param("desde") Instant desde,
            Pageable pageable
    );
}
