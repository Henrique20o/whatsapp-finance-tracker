package com.wa.finance.repository;

import com.wa.finance.domain.Transacao;
import com.wa.finance.dto.GastoPorCategoriaDTO;
import com.wa.finance.dto.mobile.ResumoCategoriaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {
    Optional<Transacao> findByExternalMessageId(String externalMessageId);

    Optional<Transacao> findByIdAndUsuarioTelefoneHash(Long id, String telefoneHash);

    @Query("""
            select coalesce(sum(t.valor), 0) from Transacao t
             where t.usuario.id = :usuarioId and t.deletado = false
               and t.dataHora >= :inicio and t.dataHora < :fimExclusivo
            """)
    BigDecimal somarMobileNoPeriodo(@Param("usuarioId") Long usuarioId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("""
            select count(t) from Transacao t
             where t.usuario.id = :usuarioId and t.deletado = false
               and t.dataHora >= :inicio and t.dataHora < :fimExclusivo
            """)
    long contarMobileNoPeriodo(@Param("usuarioId") Long usuarioId,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("""
            select new com.wa.finance.dto.mobile.ResumoCategoriaProjection(
                t.categoria.id, t.categoria.nome, sum(t.valor), count(t))
              from Transacao t
             where t.usuario.id = :usuarioId and t.deletado = false
               and t.dataHora >= :inicio and t.dataHora < :fimExclusivo
             group by t.categoria.id, t.categoria.nome order by sum(t.valor) desc
            """)
    List<ResumoCategoriaProjection> resumirMobilePorCategoria(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query(value = """
            SELECT CAST(data_hora AS date) AS data, SUM(valor) AS total, COUNT(*) AS quantidade
              FROM tb_transacao
             WHERE usuario_id = :usuarioId AND deletado = false
               AND data_hora >= :inicio AND data_hora < :fimExclusivo
             GROUP BY CAST(data_hora AS date)
            """, nativeQuery = true)
    List<MobileDailyTotalProjection> resumirMobilePorDia(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo);

    @Query("""
            select coalesce(sum(t.valor), 0)
            from Transacao t
            where t.usuario.telefoneHash = :telefoneHash
              and t.deletado = false
              and t.dataHora >= :inicio
            """)
    BigDecimal somarGastosDesde(
            @Param("telefoneHash") String telefoneHash,
            @Param("inicio") LocalDateTime inicio
    );

    @Query("""
            select new com.wa.finance.dto.GastoPorCategoriaDTO(t.categoria.id, t.categoria.nome, sum(t.valor))
            from Transacao t
            where t.usuario.telefoneHash = :telefoneHash
              and t.deletado = false
              and t.dataHora >= :inicio
            group by t.categoria.id, t.categoria.nome
            order by sum(t.valor) desc
            """)
    List<GastoPorCategoriaDTO> somarGastosPorCategoriaDesde(
            @Param("telefoneHash") String telefoneHash,
            @Param("inicio") LocalDateTime inicio
    );

    @Query("""
            select coalesce(sum(t.valor), 0)
            from Transacao t
            where t.usuario.telefoneHash = :telefoneHash
              and t.deletado = false
              and t.dataHora >= :inicio
              and t.dataHora < :fimExclusivo
            """)
    BigDecimal somarGastosNoPeriodo(
            @Param("telefoneHash") String telefoneHash,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo
    );

    @Query("""
            select new com.wa.finance.dto.GastoPorCategoriaDTO(t.categoria.id, t.categoria.nome, sum(t.valor))
            from Transacao t
            where t.usuario.telefoneHash = :telefoneHash
              and t.deletado = false
              and t.dataHora >= :inicio
              and t.dataHora < :fimExclusivo
            group by t.categoria.id, t.categoria.nome
            order by sum(t.valor) desc
            """)
    List<GastoPorCategoriaDTO> somarGastosPorCategoriaNoPeriodo(
            @Param("telefoneHash") String telefoneHash,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo
    );

    @Query("""
            select t
            from Transacao t
            where t.usuario.telefoneHash = :telefoneHash
              and t.deletado = false
              and t.categoria.id = :categoriaId
              and t.dataHora >= :inicio
              and t.dataHora < :fimExclusivo
            order by t.dataHora desc, t.id desc
            """)
    Page<Transacao> buscarPorCategoriaNoPeriodo(
            @Param("telefoneHash") String telefoneHash,
            @Param("categoriaId") Long categoriaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(t.valor), 0)
            from Transacao t
            where t.usuario.telefoneHash = :telefoneHash
              and t.deletado = false
              and t.categoria.id = :categoriaId
              and t.dataHora >= :inicio
              and t.dataHora < :fimExclusivo
            """)
    BigDecimal somarCategoriaNoPeriodo(
            @Param("telefoneHash") String telefoneHash,
            @Param("categoriaId") Long categoriaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo
    );
}
