package com.amrita.amritabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.amrita.amritabackend" })
public class AmritaBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(AmritaBackendApplication.class, args);
	}
}
