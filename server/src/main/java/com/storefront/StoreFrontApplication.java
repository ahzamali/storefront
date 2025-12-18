package com.storefront;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StoreFrontApplication {

	public static void main(String[] args) {
		SpringApplication.run(StoreFrontApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner dataSeeder(com.storefront.service.AuthService authService) {
		return args -> {
			try {
				authService.login("admin_store", "pass");
			} catch (Exception e) {
				authService.register("admin_store", "pass", com.storefront.model.Role.ADMIN);
			}
		};
	}
}
