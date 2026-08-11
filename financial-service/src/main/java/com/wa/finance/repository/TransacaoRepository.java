package com.wa.finance.repository;

import com.wa.finance.domain.Transacao;
import com.wa.finance.dto.GastoPorCategoriaDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    Optional<Transacao> findByExternalMessageId(String externalMessageId);

    @Query("""
            select coalesce(sum(t.valor), 0)
            from Transacao t
            where t.usuario.telefone = :telefone
              and t.deletado = false
              and t.dataHora >= :inicio
            """)
    BigDecimal somarGastosDesde(
            @Param("telefone") String telefone,
            @Param("inicio") LocalDateTime inicio
    );

    @Query("""
            select new com.wa.finance.dto.GastoPorCategoriaDTO(t.categoria.nome, sum(t.valor))
            from Transacao t
            where t.usuario.telefone = :telefone
              and t.deletado = false
              and t.dataHora >= :inicio
            group by t.categoria.nome
            order by sum(t.valor) desc
            """)
    List<GastoPorCategoriaDTO> somarGastosPorCategoriaDesde(
            @Param("telefone") String telefone,
            @Param("inicio") LocalDateTime inicio
    );
}
