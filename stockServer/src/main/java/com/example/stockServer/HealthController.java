package com.example.stockServer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Stock Server (Service 2) operationnel — gRPC port 9090, HTTP port 8081";
    }
}
