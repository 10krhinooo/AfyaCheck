package com.kimanga.afyacheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// DotenvApplicationInitializer (spring-dotenv) is registered via
// src/main/resources/META-INF/spring.factories rather than here, so it also runs under
// @SpringBootTest, which builds its own SpringApplication independently of this main().
@SpringBootApplication
@EnableScheduling
public class AfyacheckApplication {

	public static void main(String[] args) {
		SpringApplication.run(AfyacheckApplication.class, args);
	}

}
