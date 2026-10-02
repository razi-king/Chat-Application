package com.example.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// We Authenticate With Our Own JWT, So Spring's Default In-Memory User Is Not Needed
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ChatAppBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatAppBackendApplication.class, args);
	}

}
