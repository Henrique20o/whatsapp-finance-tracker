package com.wa.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialServiceApplication.class, args);
	}

}
