package com.dochiri.commerceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.dochiri")
public class CommerceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceApiApplication.class, args);
    }

}
