package com.selfinvestor.earnings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class EarningsMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EarningsMonitorApplication.class, args);
    }
}
