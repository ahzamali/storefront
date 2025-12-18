package com.storefront.service;

import com.storefront.model.AppUser;
import com.storefront.model.Role;
import com.storefront.repository.AppUserRepository;
import com.storefront.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    public AppUser register(String username, String password, Role role) {
        String encodedPassword = passwordEncoder.encode(password);
        return userRepository.save(new AppUser(username, encodedPassword, role));
    }

    public Optional<AppUser> login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        // If we reach here, auth successful
        return userRepository.findByUsername(username);
    }

    public String generateToken(AppUser user) {
        return tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    }
}
