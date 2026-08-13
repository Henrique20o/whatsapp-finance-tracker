package com.wa.finance.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
class FlywayPostgresIntegrationTest {

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

    @Test
    void deveAplicarTodasAsMigrationsEmPostgresVazioEValidarSchemaJpa() {
        List<Integer> versoes = jdbcTemplate.queryForList(
                "SELECT version::integer FROM flyway_schema_history WHERE success = true AND version IS NOT NULL ORDER BY installed_rank",
                Integer.class
        );

        assertThat(versoes).containsExactly(1, 2);
        assertThat(tabelasDaAplicacao()).containsExactlyInAnyOrder(
                "tb_usuario", "tb_categoria", "tb_transacao", "tb_outbox"
        );
    }

    @Test
    void deveCriarIndicesERestricoesCriticas() {
        assertThat(indiceExiste("uk_usuario_telefone_hash")).isTrue();
        assertThat(indiceExiste("uk_transacao_external_message_id")).isTrue();
        assertThat(indiceExiste("idx_outbox_pendente")).isTrue();

        Integer chavesEstrangeiras = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.table_constraints
                 WHERE constraint_schema = 'public'
                   AND constraint_type = 'FOREIGN KEY'
                   AND table_name IN ('tb_categoria', 'tb_transacao')
                """, Integer.class);

        assertThat(chavesEstrangeiras).isEqualTo(3);
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
}
