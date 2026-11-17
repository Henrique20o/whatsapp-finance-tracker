package com.wa.finance.repository;

import com.wa.finance.domain.SessaoMobile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoMobileRepository extends JpaRepository<SessaoMobile, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoMobile s where s.refreshTokenHash = :hash")
    Optional<SessaoMobile> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoMobile s where s.sessaoId = :sessaoId order by s.criadoEm desc")
    List<SessaoMobile> findFamiliaForUpdate(@Param("sessaoId") UUID sessaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoMobile s where s.sessaoId = :sessaoId and s.usuarioId = :usuarioId")
    List<SessaoMobile> findFamiliaDoUsuarioForUpdate(
            @Param("sessaoId") UUID sessaoId, @Param("usuarioId") Long usuarioId);

    List<SessaoMobile> findAllByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoMobile s where s.usuarioId = :usuarioId")
    List<SessaoMobile> findTodasDoUsuarioForUpdate(@Param("usuarioId") Long usuarioId);
}
