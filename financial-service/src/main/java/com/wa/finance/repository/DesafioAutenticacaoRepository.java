package com.wa.finance.repository;

import com.wa.finance.domain.DesafioAutenticacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DesafioAutenticacaoRepository extends JpaRepository<DesafioAutenticacao, UUID> {

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
