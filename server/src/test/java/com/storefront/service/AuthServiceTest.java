package com.storefront.service;

import com.storefront.model.AppUser;
import com.storefront.model.Role;
import com.storefront.model.Store;
import com.storefront.repository.AppUserRepository;
import com.storefront.repository.StoreRepository;
import com.storefront.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StoreRepository storeRepository;

    private AuthService authService;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        tokenProvider.init();
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider, storeRepository);
    }

    @Test
    void register_WithoutStore_CreatesUser() {
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        AppUser savedUser = new AppUser("user", "encoded", Role.EMPLOYEE);
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        AppUser result = authService.register("user", "password", Role.EMPLOYEE);

        assertNotNull(result);
        assertEquals("user", result.getUsername());
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void register_WithStore_AssignsStore() {
        Store store = new Store("Store1", Store.StoreType.VIRTUAL, null);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(userRepository.save(any(AppUser.class))).thenReturn(new AppUser("user", "encoded", Role.EMPLOYEE));

        AppUser result = authService.register("user", "password", Role.EMPLOYEE, 1L);

        assertNotNull(result);
        verify(storeRepository).findById(1L);
    }

    @Test
    void login_ValidCredentials_ReturnsUser() {
        AppUser user = new AppUser("user", "encoded", Role.EMPLOYEE);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

        Optional<AppUser> result = authService.login("user", "password");

        assertTrue(result.isPresent());
        assertEquals("user", result.get().getUsername());
    }

    @Test
    void login_InvalidPassword_ReturnsEmpty() {
        AppUser user = new AppUser("user", "encoded", Role.EMPLOYEE);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        Optional<AppUser> result = authService.login("user", "wrong");

        assertFalse(result.isPresent());
    }

    @Test
    void login_UserNotFound_ReturnsEmpty() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<AppUser> result = authService.login("nonexistent", "password");

        assertFalse(result.isPresent());
    }

    @Test
    void generateToken_ValidUser_ReturnsToken() {
        AppUser user = new AppUser("user", "encoded", Role.EMPLOYEE);

        String result = authService.generateToken(user);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
