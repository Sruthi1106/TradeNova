package com.ttcrypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TtcryptoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TtcryptoApplication.class, args);
    }
}
