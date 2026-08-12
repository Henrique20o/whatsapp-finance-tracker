package com.whatsapp_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"wuzapi.url=http://localhost:8080",
		"wuzapi.token=test-token",
		"app.financial-service.url=http://localhost:8084"
})
class WhatsappServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
