package com.elgris.usersapi.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CircuitHealthController {

    private final CircuitBreaker circuitBreaker;

    public CircuitHealthController() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        this.circuitBreaker = registry.circuitBreaker("usersService");
    }

    @GetMapping(value = "/circuitz", produces = "text/plain")
    public ResponseEntity<String> circuitStatus() {
        Metrics metrics = circuitBreaker.getMetrics();
        State state = circuitBreaker.getState();

        int stateValue;
        switch (state) {
            case CLOSED: stateValue = 0; break;
            case OPEN: stateValue = 1; break;
            case HALF_OPEN: stateValue = 2; break;
            default: stateValue = -1;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# HELP users_service_circuit_state Circuit breaker state\n");
        sb.append("# TYPE users_service_circuit_state gauge\n");
        sb.append("users_service_circuit_state ").append(stateValue).append("\n");

        sb.append("# HELP users_service_failed_calls_total Failed calls total\n");
        sb.append("# TYPE users_service_failed_calls_total counter\n");
        sb.append("users_service_failed_calls_total ").append(metrics.getNumberOfFailedCalls()).append("\n");

        sb.append("# HELP users_service_successful_calls_total Successful calls total\n");
        sb.append("# TYPE users_service_successful_calls_total counter\n");
        sb.append("users_service_successful_calls_total ").append(metrics.getNumberOfSuccessfulCalls()).append("\n");

        sb.append("# HELP users_service_not_permitted_calls_total Calls rejected by open circuit\n");
        sb.append("# TYPE users_service_not_permitted_calls_total counter\n");
        sb.append("users_service_not_permitted_calls_total ").append(metrics.getNumberOfNotPermittedCalls()).append("\n");

        sb.append("# HELP users_service_buffered_calls_total Buffered calls in window\n");
        sb.append("# TYPE users_service_buffered_calls_total gauge\n");
        sb.append("users_service_buffered_calls_total ").append(metrics.getNumberOfBufferedCalls()).append("\n");

        return ResponseEntity
                .ok()
                .header("Content-Type", "text/plain")
                .body(sb.toString());
    }
}
