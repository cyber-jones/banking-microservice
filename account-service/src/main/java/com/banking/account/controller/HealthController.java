package com.banking.account.controller;

import com.banking.account.dto.HealthDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health Management", description = "APIs for managing health information")
public class HealthController {

    @GetMapping
    public ResponseEntity<HealthDto.Response> getHealth() {
        return ResponseEntity.ok(HealthDto.Response.builder()
                .serviceName("AccountService")
                .version("1")
                .status(HealthDto.serviceStatus.ACTIVE)
                .build());
    }
}
