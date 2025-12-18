package com.storefront.controller;

import com.storefront.model.AppUser;
import com.storefront.model.Role;
import com.storefront.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            return authService.login(username, password)
                    .map(u -> {
                        String token = authService.generateToken(u);
                        return ResponseEntity.ok(Map.of("token", token, "role", u.getRole(), "userId", u.getId()));
                    })
                    .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        AppUser user = authService.register(
                body.get("username"),
                body.get("password"),
                Role.valueOf(body.get("role")));
        return ResponseEntity.ok(user);
    }
}
