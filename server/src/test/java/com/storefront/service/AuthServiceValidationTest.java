package com.storefront.service;

import com.storefront.model.AppUser;
import com.storefront.model.Role;
import com.storefront.repository.AppUserRepository;
import com.storefront.repository.StoreRepository;
import com.storefront.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceValidationTest {

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
    void register_DuplicateUsername_ThrowsException() {
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate username"));

        assertThrows(DataIntegrityViolationException.class, 
                () -> authService.register("existing", "password", Role.EMPLOYEE));
    }

    @Test
    void register_NullUsername_CreatesUserWithNull() {
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // Service doesn't validate - this is a gap that should be fixed
        assertDoesNotThrow(() -> authService.register(null, "password", Role.EMPLOYEE));
    }

    @Test
    void register_EmptyPassword_EncodesEmptyString() {
        when(passwordEncoder.encode("")).thenReturn("encoded_empty");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // Service doesn't validate - this is a gap that should be fixed
        assertDoesNotThrow(() -> authService.register("user", "", Role.EMPLOYEE));
    }

    @Test
    void login_NullUsername_ReturnsEmpty() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        Optional<AppUser> result = authService.login(null, "password");

        assertFalse(result.isPresent());
    }

    @Test
    void login_EmptyPassword_ReturnsEmpty() {
        AppUser user = new AppUser("user", "encoded", Role.EMPLOYEE);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("", "encoded")).thenReturn(false);

        Optional<AppUser> result = authService.login("user", "");

        assertFalse(result.isPresent());
    }

    @Test
    void updateUser_InvalidUserId_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, 
                () -> authService.getUserById(999L));
    }

    @Test
    void deleteUser_ValidId_DeletesUser() {
        doNothing().when(userRepository).deleteById(1L);

        assertDoesNotThrow(() -> authService.deleteUser(1L));

        verify(userRepository).deleteById(1L);
    }
}
