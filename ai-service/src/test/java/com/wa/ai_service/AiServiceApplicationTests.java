package com.wa.ai_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.ai.openai.api-key=test-key",
		"app.financial-service.url=http://localhost:8084"
})
class AiServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
