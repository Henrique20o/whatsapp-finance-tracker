package com.wa.finance.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.wa.finance.domain.Transacao;
import com.wa.finance.dto.mobile.CriarTransacaoMobileRequest;
import com.wa.finance.dto.mobile.TransacaoMobileResponse;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.repository.TransacaoRepository;
import com.wa.finance.repository.UsuarioRepository;
import com.wa.finance.web.mobile.IdempotencyConflictException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class MobileTransactionWriteService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MobileTransactionWriteService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String OPERATION = "CREATE_TRANSACTION";

    private final JdbcTemplate jdbcTemplate;
    private final TransacaoRepository transactionRepository;
    private final CategoriaRepository categoryRepository;
    private final UsuarioRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MobileTransactionWriteService(
            JdbcTemplate jdbcTemplate, TransacaoRepository transactionRepository,
            CategoriaRepository categoryRepository, UsuarioRepository userRepository,
            ObjectMapper objectMapper, Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public TransacaoMobileResponse criar(Long userId, String idempotencyKey, CriarTransacaoMobileRequest request) {
        UUID key;
        try {
            key = UUID.fromString(idempotencyKey);
        } catch (Exception exception) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Idempotency-Key deve ser um UUID");
        }
        validar(request);
        LocalDateTime transactionTime = request.dataHora() == null
                ? LocalDateTime.ofInstant(clock.instant(), BUSINESS_ZONE)
                : request.dataHora().atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), BUSINESS_ZONE);
        if (transactionTime.isAfter(now)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "A data do gasto não pode estar no futuro");
        }
        String keyHash = sha256(key.toString());
        String fingerprint = sha256(String.join("\n", request.valor().setScale(2).toPlainString(),
                request.descricao().trim(), request.categoriaId().toString(),
                request.dataHora() == null ? "NOW" : transactionTime.toString()));
        int reserved = jdbcTemplate.update("""
                INSERT INTO tb_idempotencia_mobile (id, usuario_id, operacao, chave_hash, fingerprint)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (usuario_id, operacao, chave_hash) DO NOTHING
                """, UUID.randomUUID(), userId, OPERATION, keyHash, fingerprint);
        if (reserved == 0) {
            StoredResult existing = jdbcTemplate.queryForObject("""
                    SELECT fingerprint, resposta_json FROM tb_idempotencia_mobile
                     WHERE usuario_id = ? AND operacao = ? AND chave_hash = ?
                    """, (rs, row) -> new StoredResult(rs.getString(1).trim(), rs.getString(2)),
                    userId, OPERATION, keyHash);
            if (existing == null || !existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException();
            }
            if (existing.responseJson() == null) {
                throw new IllegalStateException("Resultado idempotente ainda não foi concluído");
            }
            try {
                TransacaoMobileResponse response = objectMapper.readValue(
                        existing.responseJson(), TransacaoMobileResponse.class);
                LOGGER.info("event=MOBILE_TRANSACTION_IDEMPOTENT_REPLAY transacaoId={} correlationId={}",
                        response.id(), org.slf4j.MDC.get("correlationId"));
                return response;
            } catch (JacksonException exception) {
                throw new IllegalStateException("Resultado idempotente persistido inválido", exception);
            }
        }

        var category = categoryRepository.findByIdAndUsuario_IdAndAtivaTrue(request.categoriaId(), userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Categoria não encontrada"));
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuário não encontrado"));

        Transacao transaction = new Transacao();
        transaction.setValor(request.valor().setScale(2));
        transaction.setDescricao(request.descricao().trim());
        transaction.setDataHora(transactionTime);
        transaction.setDeletado(false);
        transaction.setOrigem(Transacao.Origem.MOBILE);
        transaction.setUsuario(user);
        transaction.setCategoria(category);
        transaction = transactionRepository.saveAndFlush(transaction);
        TransacaoMobileResponse response = TransacaoMobileResponse.from(transaction);
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            jdbcTemplate.update("""
                    UPDATE tb_idempotencia_mobile
                       SET transacao_id = ?, resposta_json = ?
                     WHERE usuario_id = ? AND operacao = ? AND chave_hash = ?
                    """, transaction.getId(), responseJson, userId, OPERATION, keyHash);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Não foi possível serializar o resultado da transação", exception);
        }
        LOGGER.info("event=MOBILE_TRANSACTION_CREATED transacaoId={} origem=MOBILE correlationId={}",
                transaction.getId(), org.slf4j.MDC.get("correlationId"));
        return response;
    }

    private void validar(CriarTransacaoMobileRequest request) {
        if (request == null || request.valor() == null || request.valor().signum() <= 0
                || request.valor().scale() > 2 || request.valor().precision() > 15
                || request.descricao() == null || request.descricao().isBlank()
                || request.descricao().trim().length() > 255 || request.categoriaId() == null
                || request.categoriaId() <= 0) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Dados da transação inválidos");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private record StoredResult(String fingerprint, String responseJson) { }
}
