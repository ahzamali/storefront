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

        // Prepare error response (consistent type with success)
        java.util.HashMap<String, Object> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Invalid credentials");

        try {
            return authService.login(username, password)
                    .map(u -> {
                        System.out.println("=== CONTROLLER DEBUG ===");
                        System.out.println("Login successful, generating token...");
                        String token = authService.generateToken(u);
                        System.out.println("Token generated: " + token);
                        Long storeId = u.getStore() != null ? u.getStore().getId() : null;
                        System.out.println("StoreId: " + storeId);

                        // Use HashMap instead of Map.of() because Map.of() doesn't accept null values
                        java.util.HashMap<String, Object> response = new java.util.HashMap<>();
                        response.put("token", token);
                        response.put("role", u.getRole());
                        response.put("userId", u.getId());
                        response.put("storeId", storeId);

                        System.out.println("Returning response: " + response);
                        System.out.println("========================");
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.status(401).body(errorResponse));
        } catch (Exception e) {
            System.err.println("=== LOGIN ERROR ===");
            System.err.println("Exception during login: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("===================");
            return ResponseEntity.status(401).body(errorResponse);
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
