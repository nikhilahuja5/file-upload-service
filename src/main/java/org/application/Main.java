package org.application;

import org.application.config.R2Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@ComponentScan(basePackages = {"org.application"})
@EnableConfigurationProperties(R2Properties.class)
@EnableScheduling
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class,args);
    }
}