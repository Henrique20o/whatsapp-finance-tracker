package com.wa.finance.repository;

import com.wa.finance.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNomeIgnoreCaseAndUsuarioIdAndAtivaTrue(String nome, Long usuarioId);

}