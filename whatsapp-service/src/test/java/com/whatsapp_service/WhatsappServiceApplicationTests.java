package com.whatsapp_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"wuzapi.url=http://localhost:8080",
		"wuzapi.token=test-token",
		"app.security.wuzapi-hmac-key=chave-hmac-de-teste-com-32-caracteres",
		"app.financial-service.url=http://localhost:8084",
		"spring.rabbitmq.listener.simple.auto-startup=false"
})
class WhatsappServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
