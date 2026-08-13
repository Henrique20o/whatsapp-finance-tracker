package com.wa.finance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.security.phone-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
		"app.security.internal-api-key=chave-interna-financeiro-de-teste-123456",
		"spring.flyway.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:financial-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"app.outbox.enabled=false"
})
class FinancialServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
