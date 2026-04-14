package com.banking.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Discovery Server.
 *
 * TEACHING POINT: In a microservices architecture, services need to find each other
 * dynamically. Eureka acts as a "phone book" — each service registers itself here
 * on startup, and other services look up addresses by service name (not hardcoded URLs).
 *
 * Key annotations:
 *   @SpringBootApplication  — enables auto-configuration, component scanning, config
 *   @EnableEurekaServer     — turns this app into a registry server
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
