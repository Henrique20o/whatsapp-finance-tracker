package com.whatsapp_service.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDate;
import java.util.List;

@Service
public class ConversationStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final Map<String, ConversationState> states = new ConcurrentHashMap<>();
    private final Map<String, ReportContext> reportContexts = new ConcurrentHashMap<>();

    public void aguardarRegistroDeGasto(String telefone) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_GASTO,
                Instant.now().plus(STATE_TTL),
                null
        ));
    }

    public boolean consumirSeAguardandoGasto(String telefone) {
        ConversationState state = states.remove(telefone);

        return state != null
                && state.step() == ConversationStep.AGUARDANDO_GASTO
                && state.expiresAt().isAfter(Instant.now());
    }

    public void aguardarNomeDaCategoria(String telefone) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_NOME_CATEGORIA,
                Instant.now().plus(STATE_TTL),
                null
        ));
    }

    public boolean consumirSeAguardandoNomeDaCategoria(String telefone) {
        ConversationState state = states.get(telefone);
        if (state == null) {
            return false;
        }

        if (state.step() != ConversationStep.AGUARDANDO_NOME_CATEGORIA
                || !state.expiresAt().isAfter(Instant.now())) {
            if (!state.expiresAt().isAfter(Instant.now())) {
                states.remove(telefone, state);
            }
            return false;
        }

        states.remove(telefone, state);
        return true;
    }

    public void aguardarCategoriaParaDesativar(String telefone) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_CATEGORIA_PARA_DESATIVAR,
                Instant.now().plus(STATE_TTL),
                null
        ));
    }

    public boolean estaAguardandoCategoriaParaDesativar(String telefone) {
        return consultarPasso(telefone, ConversationStep.AGUARDANDO_CATEGORIA_PARA_DESATIVAR) != null;
    }

    public void aguardarConfirmacaoDeDesativacao(String telefone, String categoria) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_CONFIRMACAO_DESATIVACAO,
                Instant.now().plus(STATE_TTL),
                categoria
        ));
    }

    public String consumirCategoriaParaConfirmarDesativacao(String telefone) {
        ConversationState state = consumirPasso(
                telefone,
                ConversationStep.AGUARDANDO_CONFIRMACAO_DESATIVACAO
        );
        return state == null ? null : state.contexto();
    }

    public void cancelarFluxo(String telefone) {
        states.remove(telefone);
        reportContexts.remove(telefone);
    }

    public void armazenarContextoRelatorio(
            String telefone,
            LocalDate dataInicio,
            LocalDate dataFim,
            String tituloPeriodo,
            List<ReportCategoryOption> categorias
    ) {
        reportContexts.put(telefone, new ReportContext(
                dataInicio,
                dataFim,
                tituloPeriodo,
                List.copyOf(categorias),
                Instant.now().plus(STATE_TTL)
        ));
    }

    public List<ReportCategoryOption> buscarCategoriasDoRelatorio(String telefone) {
        ReportContext context = consultarContextoRelatorio(telefone);
        return context == null ? List.of() : context.categorias();
    }

    public ReportCategorySelection selecionarCategoriaDoRelatorio(String telefone, long categoriaId) {
        ReportContext context = consultarContextoRelatorio(telefone);
        if (context == null) {
            return null;
        }
        return context.categorias().stream()
                .filter(categoria -> categoria.id() == categoriaId)
                .findFirst()
                .map(categoria -> new ReportCategorySelection(
                        context.dataInicio(),
                        context.dataFim(),
                        categoria.id(),
                        categoria.nome()
                ))
                .orElse(null);
    }

    public ReportPeriod buscarPeriodoDoRelatorio(String telefone) {
        ReportContext context = consultarContextoRelatorio(telefone);
        return context == null
                ? null
                : new ReportPeriod(
                        context.dataInicio(), context.dataFim(), context.tituloPeriodo());
    }

    private ReportContext consultarContextoRelatorio(String telefone) {
        ReportContext context = reportContexts.get(telefone);
        if (context == null) {
            return null;
        }
        if (!context.expiresAt().isAfter(Instant.now())) {
            reportContexts.remove(telefone, context);
            return null;
        }
        return context;
    }

    private ConversationState consumirPasso(String telefone, ConversationStep passo) {
        ConversationState state = consultarPasso(telefone, passo);
        if (state != null) {
            states.remove(telefone, state);
        }
        return state;
    }

    private ConversationState consultarPasso(String telefone, ConversationStep passo) {
        ConversationState state = states.get(telefone);
        if (state == null || state.step() != passo || !state.expiresAt().isAfter(Instant.now())) {
            if (state != null && !state.expiresAt().isAfter(Instant.now())) {
                states.remove(telefone, state);
            }
            return null;
        }
        return state;
    }

    enum ConversationStep {
        AGUARDANDO_GASTO,
        AGUARDANDO_NOME_CATEGORIA,
        AGUARDANDO_CATEGORIA_PARA_DESATIVAR,
        AGUARDANDO_CONFIRMACAO_DESATIVACAO
    }

    record ConversationState(ConversationStep step, Instant expiresAt, String contexto) {}

    record ReportContext(
            LocalDate dataInicio,
            LocalDate dataFim,
            String tituloPeriodo,
            List<ReportCategoryOption> categorias,
            Instant expiresAt
    ) {}

    public record ReportCategorySelection(
            LocalDate dataInicio,
            LocalDate dataFim,
            long categoriaId,
            String categoria
    ) {}

    public record ReportCategoryOption(long id, String nome) {}

    public record ReportPeriod(
            LocalDate dataInicio,
            LocalDate dataFim,
            String tituloPeriodo
    ) {}
}
