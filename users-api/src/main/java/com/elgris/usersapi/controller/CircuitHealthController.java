package com.elgris.usersapi.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CircuitHealthController {

    private final CircuitBreaker circuitBreaker;

    public CircuitHealthController(CircuitBreakerRegistry registry) {
        // "usersService" es el nombre que usás en application.properties
        this.circuitBreaker = registry.circuitBreaker("usersService");
    }

    @GetMapping("/circuitz")
    public ResponseEntity<String> circuitStatus() {
        CircuitBreaker.State state = circuitBreaker.getState();

        // Prometheus format
        StringBuilder sb = new StringBuilder();
        sb.append("# TYPE users_service_circuit_state gauge\n");

        int stateValue;
        switch (state) {
            case CLOSED:
                stateValue = 0;
                break;
            case OPEN:
                stateValue = 1;
                break;
            case HALF_OPEN:
                stateValue = 2;
                break;
            default:
                stateValue = -1;
        }


        sb.append("users_service_circuit_state ").append(stateValue).append("\n");

        return ResponseEntity.ok()
                .header("Content-Type", "text/plain")
                .body(sb.toString());
    }
}
