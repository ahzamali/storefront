package com.storefront.service;

import com.storefront.model.AppUser;
import com.storefront.model.Role;
import com.storefront.repository.AppUserRepository;
import com.storefront.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final com.storefront.repository.StoreRepository storeRepository;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            com.storefront.repository.StoreRepository storeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.storeRepository = storeRepository;
    }

    public AppUser register(String username, String password, Role role) {
        return register(username, password, role, null);
    }

    public AppUser register(String username, String password, Role role, Long storeId) {
        String encodedPassword = passwordEncoder.encode(password);
        AppUser user = new AppUser(username, encodedPassword, role);

        if (storeId != null) {
            storeRepository.findById(storeId).ifPresent(store -> {
                user.addStore(store);
            });
        }
        return userRepository.save(user);
    }

    public java.util.List<AppUser> getAllUsers() {
        return (java.util.List<AppUser>) userRepository.findAll();
    }

    public Optional<AppUser> login(String username, String password) {
        // Manual password verification instead of AuthenticationManager
        // AuthenticationManager was not working correctly in production
        System.out.println("=== LOGIN DEBUG ===");
        System.out.println("Attempting login for username: " + username);

        Optional<AppUser> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("User NOT found in database!");
            return Optional.empty();
        }

        AppUser user = userOpt.get();
        System.out.println("User found! Stored hash: " + user.getPasswordHash());
        System.out.println("Input password: " + password);

        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        System.out.println("Password matches: " + matches);
        System.out.println("===================");

        return matches ? Optional.of(user) : Optional.empty();
    }

    public String generateToken(AppUser user) {
        return tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
