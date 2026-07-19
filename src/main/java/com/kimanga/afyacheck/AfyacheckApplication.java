package com.kimanga.afyacheck;

import me.paulschwarz.springdotenv.spring.DotenvApplicationInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class AfyacheckApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(AfyacheckApplication.class)
				.initializers(new DotenvApplicationInitializer())
				.run(args);
	}

}
