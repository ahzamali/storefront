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

                        // Get list of store IDs
                        java.util.List<Long> storeIds = u.getStores().stream()
                                .map(com.storefront.model.Store::getId)
                                .collect(java.util.stream.Collectors.toList());
                        System.out.println("Store IDs: " + storeIds);

                        // Use HashMap instead of Map.of() because Map.of() doesn't accept null values
                        java.util.HashMap<String, Object> response = new java.util.HashMap<>();
                        response.put("token", token);
                        response.put("role", u.getRole());
                        response.put("userId", u.getId());
                        response.put("storeIds", storeIds);

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

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody com.storefront.dto.UserUpdateDTO body) {
        // RBAC Logic
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        String currentUsername = auth.getName();

        // Find current user to check ID and Role
        // Ideally we cache this or get from principal, but for now we fetch
        // Since we don't have easy access to current user ID from principal name
        // without lookup or custom principal
        // Let's rely on AuthService to help or just lookup here
        // Simpler: Check if username matches target user's username?
        // Or fetch current user.

        // Note: For this iteration, let's just fetch current user by username
        // (Assuming username is unique and present)
        // Optimization: In real app, Principal should have ID.

        // Use a service method (not exposed in interface yet, but repo has it)
        // We'll trust AuthService to handle simple lookup or just add a helper if
        // needed.
        // But for now, let's assume we can't easily get ID from name without repo
        // access.
        // Let's inject repo or just check permissions based on role for other users.

        boolean isSelf = false;
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        // Retrieve target user to verify identity match if not super admin
        // But wait, we can't query repo from controller ideally?
        // Actually we can just pass the check logic to service or do it here.
        // Let's do it here with a quick trick:
        // If !isSuperAdmin, we MUST ensure target ID belongs to currentUsername.

        // This requires fetching the target user to see if username matches token
        // username.
        // OR fetching current user to compare IDs.

        // Let's assume we can fetch target user via AuthService (we need a getById
        // exposed? no, we have listUsers)
        // Let's add getById to AuthService quickly or use existing listUsers loop
        // (inefficient but works)
        // Better: Just add getById to AuthService or public access.

        // Actually, we can use the existing `updateUser` in service to verify ownership
        // if we pass the current username?
        // No, separation of concerns.

        // Re-strategy: Allow update if (isSuperAdmin OR isSelf).

        // Let's verify "isSelf" by getting the target user.
        // We really need `getUserById` in AuthService. I'll add that helper now
        // implicitly or just use the repo if I had it.
        // Since I don't want to change AuthService signature too much, I'll use the
        // hack:
        // Assume ID match if I can get current ID.

        // Alternative: Pass `currentUsername` to `updateUser` and let Service decide?
        // No, Controller should handle HTTP status.

        // I will trust that the user calling this knows their ID? No security risk.
        // CORRECT PATH: Fetch DB user for `id` and compare username.
        // I will assume I can inject repository here? Or just add `getUser(id)` to
        // Service.
        // Let's add `getUser(id)` to Service in next step if needed, OR just use
        // `listUsers()` stream for now (MVP).

        try {
            AppUser targetUser = authService.getUserById(id); // I need to add this
            if (!targetUser.getUsername().equals(currentUsername) && !isSuperAdmin) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            // Specific Rule: Non-SuperAdmin cannot change stores
            if (!isSuperAdmin && body.getStoreIds() != null) {
                return ResponseEntity.status(403).body(Map.of("error", "Only Super Admin can assign stores"));
            }

            AppUser updated = authService.updateUser(id, body);
            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
    }
}
