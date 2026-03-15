package com.quantum.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.quantum.auth", "com.quantum.jwt" })
public class QuantumAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantumAuthApplication.class, args);
    }
}
