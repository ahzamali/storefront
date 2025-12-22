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
                        Long storeId = u.getStore() != null ? u.getStore().getId() : null;
                        return ResponseEntity.ok(
                                Map.of("token", token, "role", u.getRole(), "userId", u.getId(), "storeId", storeId));
                    })
                    .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_STORE_ADMIN')")
    public ResponseEntity<?> register(@RequestBody com.storefront.dto.RegisterRequestDTO body) {
        try {
            Long storeId = body.getStoreId();
            Role role = Role.valueOf(body.getRole());

            AppUser user = authService.register(
                    body.getUsername(),
                    body.getPassword(),
                    role,
                    storeId);
            return ResponseEntity.ok(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Role or Data"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_STORE_ADMIN')")
    public java.util.List<AppUser> listUsers() {
        return authService.getAllUsers();
    }

    @DeleteMapping("/users/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_STORE_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
