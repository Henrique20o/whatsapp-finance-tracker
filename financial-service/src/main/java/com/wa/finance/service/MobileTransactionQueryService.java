package com.wa.finance.service;

import com.wa.finance.dto.mobile.TransacaoMobileResponse;
import com.wa.finance.dto.mobile.TransacoesMobilePageResponse;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class MobileTransactionQueryService {
    private final TransacaoRepository transactions;
    private final CategoriaRepository categories;

    public MobileTransactionQueryService(TransacaoRepository transactions, CategoriaRepository categories) {
        this.transactions = transactions;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public TransacoesMobilePageResponse listar(
            Long usuarioId, LocalDate inicio, LocalDate fim, Long categoriaId, int pagina, int tamanho
    ) {
        if ((inicio == null) != (fim == null) || pagina < 0 || tamanho < 1 || tamanho > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros ou paginação inválidos");
        }
        if (inicio != null && (fim.isBefore(inicio) || inicio.plusYears(1).isBefore(fim))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido");
        }
        if (categoriaId != null && (categoriaId <= 0
                || categories.findByIdAndUsuario_Id(categoriaId, usuarioId).isEmpty())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada");
        }
        LocalDateTime from = inicio == null ? null : inicio.atStartOfDay();
        LocalDateTime until = fim == null ? null : fim.plusDays(1).atStartOfDay();
        Specification<com.wa.finance.domain.Transacao> filters = (root, query, criteria) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(criteria.equal(root.get("usuario").get("id"), usuarioId));
            predicates.add(criteria.isFalse(root.get("deletado")));
            if (from != null) predicates.add(criteria.greaterThanOrEqualTo(root.get("dataHora"), from));
            if (until != null) predicates.add(criteria.lessThan(root.get("dataHora"), until));
            if (categoriaId != null) predicates.add(criteria.equal(root.get("categoria").get("id"), categoriaId));
            return criteria.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var sort = Sort.by(Sort.Order.desc("dataHora"), Sort.Order.desc("id"));
        var result = transactions.findAll(filters, PageRequest.of(pagina, tamanho, sort));
        return new TransacoesMobilePageResponse(
                result.getContent().stream().map(TransacaoMobileResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
