package com.wa.finance.outbox;

import com.wa.finance.dto.RespostaUsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.security.phone-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:outbox-transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.outbox.enabled=false"
})
class OutboxTransactionIntegrationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void devePersistirOutboxNoCommit() {
        transactionTemplate.executeWithoutResult(status -> outboxService.adicionarConfirmacao(resposta()));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deveRemoverOutboxNoRollbackDaTransacaoFinanceira() {
        transactionTemplate.executeWithoutResult(status -> {
            outboxService.adicionarConfirmacao(resposta());
            status.setRollbackOnly();
        });

        assertThat(repository.count()).isZero();
    }

    private RespostaUsuarioDTO resposta() {
        return new RespostaUsuarioDTO("5531999998888", "Gasto registrado", 99L);
    }
}
