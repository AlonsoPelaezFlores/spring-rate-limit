package com.github.alonsopelaezflores.testapp;

import com.github.alonsopelaezflores.ratelimit.RateLimit;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    @RateLimit(name = "test-endpoint")
    public void limitMethod() {
        System.out.println("Ejecutando método limitado...");
    }
    @RateLimit(name = "login-endpoint", keyParamIndex = 0)
    public void login(String userId) {
        System.out.println("Login de: " + userId);
    }
    @RateLimit(name = "payment-endpoint", keyParamIndex = 0)
    public void pay(String userId) {
        System.out.println("Pago de: " + userId);
    }
}
