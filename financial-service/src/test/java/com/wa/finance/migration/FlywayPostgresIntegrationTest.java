package com.wa.finance.migration;

import com.wa.finance.MobileSecurityIntegrationTest;
import com.wa.finance.domain.DesafioAutenticacao;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.PhoneProtectionService;
import com.wa.finance.service.DesafioAutenticacaoService;
import com.wa.finance.service.DesafioCriado;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "app.security.phone-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.internal-api-key=chave-interna-financeiro-de-teste-123456",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.outbox.enabled=false"
})
@AutoConfigureMockMvc
class FlywayPostgresIntegrationTest extends MobileSecurityIntegrationTest {

    private static final Pattern CHALLENGE_ID_PATTERN =
            Pattern.compile("\\\"challengeId\\\":\\\"([^\\\"]+)\\\"");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("db_wa_finance_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DesafioAutenticacaoService desafioService;

    @Autowired
    private DesafioAutenticacaoRepository desafioRepository;

    @Autowired
    private PhoneProtectionService phoneProtectionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveAplicarTodasAsMigrationsEmPostgresVazioEValidarSchemaJpa() {
        List<Integer> versoes = jdbcTemplate.queryForList(
                "SELECT version::integer FROM flyway_schema_history WHERE success = true AND version IS NOT NULL ORDER BY installed_rank",
                Integer.class
        );

        assertThat(versoes).containsExactly(1, 2, 3);
        assertThat(tabelasDaAplicacao()).containsExactlyInAnyOrder(
                "tb_usuario", "tb_categoria", "tb_transacao", "tb_outbox",
                "tb_desafio_autenticacao", "tb_sessao_mobile"
        );

        assertThat(tipoDaColuna("tb_desafio_autenticacao", "id")).isEqualTo("uuid");
        assertThat(tipoDaColuna("tb_desafio_autenticacao", "expira_em"))
                .isEqualTo("timestamp with time zone");
        assertThat(tipoDaColuna("tb_sessao_mobile", "sessao_id")).isEqualTo("uuid");
        assertThat(tipoDaColuna("tb_sessao_mobile", "expira_absoluto_em"))
                .isEqualTo("timestamp with time zone");
    }

    @Test
    void deveCriarIndicesERestricoesCriticas() {
        assertThat(indiceExiste("uk_usuario_telefone_hash")).isTrue();
        assertThat(indiceExiste("uk_transacao_external_message_id")).isTrue();
        assertThat(indiceExiste("idx_outbox_pendente")).isTrue();
        assertThat(indiceExiste("uk_desafio_auth_telefone_ativo")).isTrue();
        assertThat(indiceExiste("idx_desafio_auth_telefone_criado")).isTrue();
        assertThat(indiceExiste("idx_desafio_auth_ip_criado")).isTrue();
        assertThat(indiceExiste("idx_desafio_auth_expiracao")).isTrue();
        assertThat(indiceExiste("uk_sessao_mobile_refresh_hash")).isTrue();
        assertThat(indiceExiste("uk_sessao_mobile_token_anterior")).isTrue();
        assertThat(indiceExiste("uk_sessao_mobile_familia_ativa")).isTrue();
        assertThat(indiceExiste("idx_sessao_mobile_familia")).isTrue();
        assertThat(indiceExiste("idx_sessao_mobile_usuario_ativa")).isTrue();
        assertThat(indiceExiste("idx_sessao_mobile_expiracao")).isTrue();

        Integer chavesEstrangeiras = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.table_constraints
                 WHERE constraint_schema = 'public'
                   AND constraint_type = 'FOREIGN KEY'
                   AND table_name IN ('tb_categoria', 'tb_transacao')
                """, Integer.class);

        assertThat(chavesEstrangeiras).isEqualTo(3);
        assertThat(constraintExiste("tb_sessao_mobile", "fk_sessao_mobile_usuario", "FOREIGN KEY"))
                .isTrue();
        assertThat(constraintExiste("tb_sessao_mobile", "fk_sessao_mobile_token_anterior", "FOREIGN KEY"))
                .isTrue();
        assertThat(constraintExiste("tb_desafio_autenticacao", "ck_desafio_auth_tentativas", "CHECK"))
                .isTrue();
        assertThat(constraintExiste("tb_sessao_mobile", "ck_sessao_mobile_revogacao", "CHECK"))
                .isTrue();
    }

    @Test
    void deveAplicarRegrasDoCicloDeVidaDoDesafio() {
        String telefoneHash = hash('a');
        UUID primeiroDesafio = UUID.randomUUID();

        inserirDesafio(primeiroDesafio, telefoneHash, hash('b'));

        assertThatThrownBy(() -> inserirDesafio(UUID.randomUUID(), telefoneHash, hash('c')))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update("""
                UPDATE tb_desafio_autenticacao
                   SET invalidado_em = CURRENT_TIMESTAMP
                 WHERE id = ?
                """, primeiroDesafio);

        inserirDesafio(UUID.randomUUID(), telefoneHash, hash('d'));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tb_desafio_autenticacao (
                    id, telefone_criptografado, telefone_hash, codigo_hmac, ip_hash,
                    tentativas, expira_em, reenvio_permitido_em
                ) VALUES (?, ?, ?, ?, ?, 6, CURRENT_TIMESTAMP + INTERVAL '5 minutes',
                          CURRENT_TIMESTAMP + INTERVAL '1 minute')
                """, UUID.randomUUID(), "telefone-protegido", hash('e'), hash('f'), hash('1')))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE tb_desafio_autenticacao
                   SET consumido_em = CURRENT_TIMESTAMP,
                       invalidado_em = CURRENT_TIMESTAMP
                 WHERE id = ?
                """, primeiroDesafio))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void devePermitirRotacaoEImpedirRamificacoesNaSessao() {
        long usuarioId = inserirUsuario();
        UUID sessaoId = UUID.randomUUID();
        UUID tokenInicial = UUID.randomUUID();

        inserirGeracaoSessao(tokenInicial, sessaoId, usuarioId, hash('2'), null);

        assertThatThrownBy(() -> inserirGeracaoSessao(
                UUID.randomUUID(), sessaoId, usuarioId, hash('3'), null))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update("""
                UPDATE tb_sessao_mobile
                   SET usado_em = CURRENT_TIMESTAMP
                 WHERE id = ?
                """, tokenInicial);

        UUID tokenRotacionado = UUID.randomUUID();
        inserirGeracaoSessao(tokenRotacionado, sessaoId, usuarioId, hash('4'), tokenInicial);

        assertThatThrownBy(() -> inserirGeracaoSessao(
                UUID.randomUUID(), sessaoId, usuarioId, hash('5'), tokenInicial))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_sessao_mobile WHERE sessao_id = ?",
                Integer.class,
                sessaoId
        )).isEqualTo(2);
    }

    @Test
    void deveImpedirVinculosInvalidosEVazamentoEntreFamilias() {
        long usuarioId = inserirUsuario();
        UUID sessaoId = UUID.randomUUID();
        UUID tokenInicial = UUID.randomUUID();

        inserirGeracaoSessao(tokenInicial, sessaoId, usuarioId, hash('6'), null);
        jdbcTemplate.update(
                "UPDATE tb_sessao_mobile SET usado_em = CURRENT_TIMESTAMP WHERE id = ?",
                tokenInicial
        );

        assertThatThrownBy(() -> inserirGeracaoSessao(
                UUID.randomUUID(), UUID.randomUUID(), usuarioId, hash('7'), tokenInicial))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> inserirGeracaoSessao(
                UUID.randomUUID(), UUID.randomUUID(), Long.MAX_VALUE, hash('8'), null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tb_sessao_mobile (
                    id, sessao_id, usuario_id, refresh_token_hash,
                    expira_inatividade_em, expira_absoluto_em, motivo_revogacao
                ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '30 days',
                          CURRENT_TIMESTAMP + INTERVAL '90 days', 'LOGOUT')
                """, UUID.randomUUID(), UUID.randomUUID(), usuarioId, hash('9')))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveSerializarCriacoesConcorrentesDoMesmoTelefone() throws Exception {
        String telefone = "5511987654321";
        String telefoneHash = phoneProtectionService.lookupHash(telefone);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<DesafioCriado> first = executor.submit(
                    () -> criarDepoisDoSinal(telefone, "192.0.2.10", prontas, iniciar)
            );
            Future<DesafioCriado> second = executor.submit(
                    () -> criarDepoisDoSinal(telefone, "192.0.2.11", prontas, iniciar)
            );

            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            DesafioCriado primeiroResultado = first.get(30, TimeUnit.SECONDS);
            DesafioCriado segundoResultado = second.get(30, TimeUnit.SECONDS);
            List<DesafioAutenticacao> desafios = desafioRepository
                    .findAllByTelefoneHashOrderByCriadoEmAsc(telefoneHash);

            assertThat(desafios).hasSize(2);
            assertThat(desafios).filteredOn(desafio -> desafio.getInvalidadoEm() == null).hasSize(1);
            assertThat(desafios).filteredOn(desafio -> desafio.getInvalidadoEm() != null).hasSize(1);
            assertThat(desafios).allSatisfy(desafio -> {
                assertThat(desafio.getCodigoHmac())
                        .matches("[0-9a-f]{64}")
                        .isNotEqualTo(primeiroResultado.getCodigoParaEnvio())
                        .isNotEqualTo(segundoResultado.getCodigoParaEnvio());
                assertThat(desafio.getTelefoneCriptografado()).isNotEqualTo(telefone);
                assertThat(phoneProtectionService.decrypt(desafio.getTelefoneCriptografado())).isEqualTo(telefone);
                assertThat(desafio.getIpHash()).matches("[0-9a-f]{64}");
            });
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void devePreservarDesafioAnteriorQuandoNovaCriacaoFalhar() {
        String telefone = "5511976543210";
        String telefoneHash = phoneProtectionService.lookupHash(telefone);
        desafioService.criar(telefone, "192.0.2.20");

        assertThatThrownBy(() -> desafioService.criar(telefone, "ip-invalido"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Endereço IP canônico inválido");

        List<DesafioAutenticacao> desafios = desafioRepository
                .findAllByTelefoneHashOrderByCriadoEmAsc(telefoneHash);
        assertThat(desafios).hasSize(1);
        assertThat(desafios.getFirst().getInvalidadoEm()).isNull();
    }

    @Test
    void endpointMobileDeveCriarUmUnicoDesafioSemCriarUsuario() throws Exception {
        String telefone = "5511965432109";
        String telefoneHash = phoneProtectionService.lookupHash(telefone);
        Integer usuariosAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_usuario", Integer.class);

        String responseBody = mockMvc.perform(post("/mobile/v1/auth/codes")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.30");
                            return request;
                        })
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"" + telefone + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Correlation-Id", matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                )))
                .andExpect(jsonPath("$.expiraEmSegundos").value(300))
                .andExpect(jsonPath("$.reenviarEmSegundos").value(60))
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var matcher = CHALLENGE_ID_PATTERN.matcher(responseBody);
        assertThat(matcher.find()).isTrue();
        UUID challengeId = UUID.fromString(matcher.group(1));
        List<DesafioAutenticacao> desafios = desafioRepository
                .findAllByTelefoneHashOrderByCriadoEmAsc(telefoneHash);

        assertThat(desafios).singleElement().satisfies(desafio -> {
            assertThat(desafio.getId()).isEqualTo(challengeId);
            assertThat(desafio.getTelefoneCriptografado()).doesNotContain(telefone);
            assertThat(phoneProtectionService.decrypt(desafio.getTelefoneCriptografado())).isEqualTo(telefone);
            assertThat(desafio.getCodigoHmac()).matches("[0-9a-f]{64}");
            assertThat(desafio.getIpHash()).matches("[0-9a-f]{64}");
        });
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_usuario", Integer.class))
                .isEqualTo(usuariosAntes);

        String outboxPayload = jdbcTemplate.queryForObject("""
                SELECT payload
                  FROM tb_outbox
                 WHERE tipo_evento = 'CODIGO_AUTENTICACAO_WHATSAPP'
                   AND payload LIKE ?
                """, String.class, "%" + challengeId + "%");
        assertThat(outboxPayload)
                .contains("\"ciphertext\"")
                .contains("\"challengeId\":\"" + challengeId + "\"")
                .doesNotContain(telefone)
                .doesNotContain("\"telefone\"")
                .doesNotContain("\"codigo\"");
    }

    private List<String> tabelasDaAplicacao() {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_name LIKE 'tb_%'
                 ORDER BY table_name
                """, String.class);
    }

    private boolean indiceExiste(String nome) {
        Integer quantidade = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                Integer.class,
                nome
        );
        return quantidade != null && quantidade == 1;
    }

    private String tipoDaColuna(String tabela, String coluna) {
        return jdbcTemplate.queryForObject("""
                SELECT data_type
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = ?
                   AND column_name = ?
                """, String.class, tabela, coluna);
    }

    private boolean constraintExiste(String tabela, String nome, String tipo) {
        Integer quantidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.table_constraints
                 WHERE constraint_schema = 'public'
                   AND table_name = ?
                   AND constraint_name = ?
                   AND constraint_type = ?
                """, Integer.class, tabela, nome, tipo);
        return quantidade != null && quantidade == 1;
    }

    private void inserirDesafio(UUID id, String telefoneHash, String ipHash) {
        jdbcTemplate.update("""
                INSERT INTO tb_desafio_autenticacao (
                    id, telefone_criptografado, telefone_hash, codigo_hmac, ip_hash,
                    expira_em, reenvio_permitido_em
                ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '5 minutes',
                          CURRENT_TIMESTAMP + INTERVAL '1 minute')
                """, id, "telefone-protegido", telefoneHash, hash('0'), ipHash);
    }

    private long inserirUsuario() {
        UUID sufixo = UUID.randomUUID();
        return jdbcTemplate.queryForObject("""
                INSERT INTO tb_usuario (telefone, telefone_hash)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, "telefone-protegido-" + sufixo, hashAleatorio());
    }

    private void inserirGeracaoSessao(
            UUID id,
            UUID sessaoId,
            long usuarioId,
            String refreshTokenHash,
            UUID tokenAnteriorId
    ) {
        jdbcTemplate.update("""
                INSERT INTO tb_sessao_mobile (
                    id, sessao_id, usuario_id, refresh_token_hash, token_anterior_id,
                    nome_dispositivo, expira_inatividade_em, expira_absoluto_em
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '30 days',
                          CURRENT_TIMESTAMP + INTERVAL '90 days')
                """, id, sessaoId, usuarioId, refreshTokenHash, tokenAnteriorId, "Dispositivo de teste");
    }

    private String hash(char caractere) {
        return String.valueOf(caractere).repeat(64);
    }

    private String hashAleatorio() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private DesafioCriado criarDepoisDoSinal(
            String telefone,
            String ip,
            CountDownLatch prontas,
            CountDownLatch iniciar
    ) throws InterruptedException {
        prontas.countDown();
        if (!iniciar.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("As criações concorrentes não receberam o sinal de início");
        }
        return desafioService.criar(telefone, ip);
    }
}
