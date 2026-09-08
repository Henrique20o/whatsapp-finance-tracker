package com.wa.finance.service;

import com.wa.finance.dto.ResumoFinanceiroDTO;
import com.wa.finance.dto.RelatorioPeriodoDTO;
import com.wa.finance.dto.DetalheCategoriaDTO;
import com.wa.finance.dto.TransacaoRelatorioDTO;
import com.wa.finance.repository.TransacaoRepository;
import com.wa.finance.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import com.wa.finance.security.PhoneProtectionService;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PhoneProtectionService phoneProtectionService;

    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarResumo(String telefone) {
        LocalDateTime agora = LocalDateTime.now(BUSINESS_ZONE);
        LocalDateTime inicioTrintaDias = agora.minusDays(30);
        String telefoneHash = phoneProtectionService.lookupHash(telefone);

        return new ResumoFinanceiroDTO(
                transacaoRepository.somarGastosDesde(telefoneHash, agora.minusDays(7)),
                transacaoRepository.somarGastosDesde(telefoneHash, inicioTrintaDias),
                transacaoRepository.somarGastosPorCategoriaDesde(telefoneHash, inicioTrintaDias)
        );
    }

    @Transactional(readOnly = true)
    public RelatorioPeriodoDTO gerarRelatorioPorPeriodo(
            String telefone,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        validarPeriodo(dataInicio, dataFim);

        String telefoneHash = phoneProtectionService.lookupHash(telefone);
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fimExclusivo = dataFim.plusDays(1).atStartOfDay();

        return new RelatorioPeriodoDTO(
                dataInicio,
                dataFim,
                transacaoRepository.somarGastosNoPeriodo(telefoneHash, inicio, fimExclusivo),
                transacaoRepository.somarGastosPorCategoriaNoPeriodo(telefoneHash, inicio, fimExclusivo)
        );
    }

    @Transactional(readOnly = true)
    public DetalheCategoriaDTO detalharCategoria(
            String telefone,
            Long categoriaId,
            LocalDate dataInicio,
            LocalDate dataFim,
            int pagina,
            int tamanho
    ) {
        validarPeriodo(dataInicio, dataFim);
        if (categoriaId == null || categoriaId <= 0) {
            throw badRequest("A categoria deve ser informada");
        }
        if (pagina < 0 || tamanho < 1 || tamanho > 20) {
            throw badRequest("A paginação informada é inválida");
        }

        String telefoneHash = phoneProtectionService.lookupHash(telefone);
        var categoria = categoriaRepository.findByIdAndUsuarioTelefoneHash(categoriaId, telefoneHash)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Categoria não encontrada"
                ));
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fimExclusivo = dataFim.plusDays(1).atStartOfDay();
        var transacoes = transacaoRepository.buscarPorCategoriaNoPeriodo(
                telefoneHash,
                categoriaId,
                inicio,
                fimExclusivo,
                PageRequest.of(pagina, tamanho)
        );

        return new DetalheCategoriaDTO(
                dataInicio,
                dataFim,
                categoria.getNome(),
                transacaoRepository.somarCategoriaNoPeriodo(
                        telefoneHash, categoriaId, inicio, fimExclusivo),
                transacoes.getTotalElements(),
                transacoes.getNumber(),
                transacoes.getTotalPages(),
                transacoes.getContent().stream().map(TransacaoRelatorioDTO::from).toList()
        );
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null || dataFim.isBefore(dataInicio)) {
            throw badRequest("O período informado é inválido");
        }
        if (dataInicio.plusYears(1).isBefore(dataFim)) {
            throw badRequest("O período máximo permitido é de um ano");
        }
    }

    private org.springframework.web.server.ResponseStatusException badRequest(String mensagem) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                mensagem
        );
    }
}
