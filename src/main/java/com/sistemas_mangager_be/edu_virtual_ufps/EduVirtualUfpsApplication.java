package com.sistemas_mangager_be.edu_virtual_ufps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EduVirtualUfpsApplication {

	public static void main(String[] args) {
		SpringApplication.run(EduVirtualUfpsApplication.class, args);
	}

}
