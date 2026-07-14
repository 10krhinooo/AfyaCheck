package com.kimanga.afyacheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AfyacheckApplication {

	public static void main(String[] args) {
		SpringApplication.run(AfyacheckApplication.class, args);
	}

}
