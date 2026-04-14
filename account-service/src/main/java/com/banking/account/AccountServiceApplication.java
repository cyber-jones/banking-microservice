package com.banking.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Account Service — manages bank accounts (create, view, close).
 *
 * TEACHING POINT — Key Spring Boot Annotations:
 *
 * @SpringBootApplication  combines three annotations:
 *   - @Configuration      marks this class as a source of bean definitions
 *   - @EnableAutoConfiguration  tells Spring Boot to auto-configure based on classpath
 *   - @ComponentScan      scans this package and sub-packages for Spring components
 *
 * @EnableDiscoveryClient  registers this service with Eureka on startup
 * @EnableFeignClients     enables declarative HTTP clients (FeignClient)
 *
 * The app auto-configures:
 *   - Embedded Tomcat web server
 *   - JPA/Hibernate with H2 database
 *   - Spring Security filter chain
 *   - Kafka producer/consumer
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
