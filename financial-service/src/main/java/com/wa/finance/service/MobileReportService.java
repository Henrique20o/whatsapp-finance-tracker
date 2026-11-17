package com.wa.finance.service;

import com.wa.finance.dto.mobile.CategoriaResumoMobileResponse;
import com.wa.finance.dto.mobile.ResumoMobileResponse;
import com.wa.finance.dto.mobile.RelatorioPorPeriodoMobileResponse;
import com.wa.finance.dto.mobile.PontoRelatorioMobile;
import com.wa.finance.repository.TransacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MobileReportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final TransacaoRepository repository;
    private final Clock clock;

    public MobileReportService(TransacaoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResumoMobileResponse resumo(Long userId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        if (start == null && end == null) {
            end = today;
            start = today.minusDays(29);
        } else if (start == null || end == null || end.isBefore(start) || start.plusYears(1).isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido");
        }
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime until = end.plusDays(1).atStartOfDay();
        BigDecimal total = repository.somarMobileNoPeriodo(userId, from, until);
        long count = repository.contarMobileNoPeriodo(userId, from, until);
        var categories = repository.resumirMobilePorCategoria(userId, from, until).stream()
                .map(item -> new CategoriaResumoMobileResponse(
                        item.categoriaId(), item.nome(), item.total(), item.quantidade(),
                        total.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                                : item.total().multiply(BigDecimal.valueOf(100))
                                .divide(total, 2, RoundingMode.HALF_UP)))
                .toList();
        return new ResumoMobileResponse(start, end, total.setScale(2, RoundingMode.HALF_UP), count, categories);
    }

    @Transactional(readOnly = true)
    public RelatorioPorPeriodoMobileResponse porPeriodo(Long userId, LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start) || start.plusYears(1).isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido");
        }
        Map<LocalDate, PontoRelatorioMobile> points = repository.resumirMobilePorDia(
                        userId, start.atStartOfDay(), end.plusDays(1).atStartOfDay()).stream()
                .collect(Collectors.toMap(
                        com.wa.finance.repository.MobileDailyTotalProjection::getData,
                        row -> new PontoRelatorioMobile(row.getData(), row.getTotal(), row.getQuantidade())));
        var daily = start.datesUntil(end.plusDays(1))
                .map(date -> points.getOrDefault(date,
                        new PontoRelatorioMobile(date, BigDecimal.ZERO.setScale(2), 0)))
                .toList();
        return new RelatorioPorPeriodoMobileResponse(start, end, daily);
    }
}
