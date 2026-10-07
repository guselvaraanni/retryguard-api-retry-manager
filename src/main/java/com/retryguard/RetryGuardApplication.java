package com.retryguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RetryGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetryGuardApplication.class, args);
    }
}
