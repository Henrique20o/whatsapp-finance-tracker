package com.wa.finance.repository;

import com.wa.finance.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("SELECT c.nome FROM Categoria c WHERE c.usuario.telefoneHash = :telefoneHash AND c.ativa = true")
    List<String> findNomesCategoriasAtivasByTelefoneHash(@Param("telefoneHash") String telefoneHash);

    Optional<Categoria> findByNomeIgnoreCaseAndUsuarioId(String nome, Long usuarioId);

    Optional<Categoria> findByNomeIgnoreCaseAndUsuarioIdAndAtivaTrue(String nome, Long usuarioId);

    Optional<Categoria> findByIdAndUsuarioTelefoneHash(Long id, String telefoneHash);

    boolean existsByNomeIgnoreCaseAndUsuarioId(String nome, Long usuarioId);

    List<Categoria> findAllByUsuario_IdAndAtivaTrueOrderByNomeAsc(Long usuarioId);

    Optional<Categoria> findByIdAndUsuario_IdAndAtivaTrue(Long id, Long usuarioId);

    Optional<Categoria> findByIdAndUsuario_Id(Long id, Long usuarioId);

    @Query(value = "SELECT * FROM tb_categoria WHERE usuario_id = :usuarioId "
            + "AND lower(regexp_replace(trim(nome), '[[:space:]]+', ' ', 'g')) "
            + "= lower(regexp_replace(trim(:nome), '[[:space:]]+', ' ', 'g')) ORDER BY id", nativeQuery = true)
    List<Categoria> findByNormalizedName(@Param("usuarioId") Long usuarioId, @Param("nome") String nome);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Categoria c where c.id = :id and c.usuario.id = :usuarioId")
    Optional<Categoria> findByIdAndUsuarioIdForUpdate(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
