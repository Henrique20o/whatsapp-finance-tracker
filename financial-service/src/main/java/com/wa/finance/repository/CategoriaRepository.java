package com.wa.finance.repository;

import com.wa.finance.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("SELECT c.nome FROM Categoria c WHERE c.usuario.telefone = :telefone AND c.ativa = true")
    List<String> findNomesCategoriasAtivasByTelefone(@Param("telefone") String telefone);

    Optional<Categoria> findByNomeIgnoreCaseAndUsuarioId(String nome, Long usuarioId);

    boolean existsByNomeIgnoreCaseAndUsuarioId(String nome, Long usuarioId);
}
