package com.ttcrypto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ApiRootController {

    @Value("${spring.application.name:ttcrypto-trading-platform}")
    private String appName;

    @GetMapping({"", "/"})
    public Map<String, Object> root() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", appName);
        response.put("status", "ok");
        response.put("message", "TT Crypto API is running");
        response.put("health", "/actuator/health");
        return response;
    }
}
