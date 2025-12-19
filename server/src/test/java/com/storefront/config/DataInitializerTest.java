package com.storefront.config;

import com.storefront.model.Store;
import com.storefront.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    @Test
    void initDatabase_shouldCreateMasterStoreAndAdmin_whenTheyDoNotExist() throws Exception {
        // Arrange
        StoreRepository storeRepository = mock(StoreRepository.class);
        com.storefront.repository.AppUserRepository userRepository = mock(
                com.storefront.repository.AppUserRepository.class);
        org.springframework.security.crypto.password.PasswordEncoder passwordEncoder = mock(
                org.springframework.security.crypto.password.PasswordEncoder.class);

        when(storeRepository.findFirstByType(Store.StoreType.MASTER)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded_pass");

        DataInitializer dataInitializer = new DataInitializer();
        CommandLineRunner runner = dataInitializer.initDatabase(storeRepository, userRepository, passwordEncoder);

        // Act
        runner.run();

        // Assert
        verify(storeRepository).save(any(Store.class));
        verify(userRepository).save(any(com.storefront.model.AppUser.class));
    }

    @Test
    void initDatabase_shouldNotCreateAnything_whenTheyAlreadyExist() throws Exception {
        // Arrange
        StoreRepository storeRepository = mock(StoreRepository.class);
        com.storefront.repository.AppUserRepository userRepository = mock(
                com.storefront.repository.AppUserRepository.class);
        org.springframework.security.crypto.password.PasswordEncoder passwordEncoder = mock(
                org.springframework.security.crypto.password.PasswordEncoder.class);

        when(storeRepository.findFirstByType(Store.StoreType.MASTER)).thenReturn(Optional.of(new Store()));
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(new com.storefront.model.AppUser()));

        DataInitializer dataInitializer = new DataInitializer();
        CommandLineRunner runner = dataInitializer.initDatabase(storeRepository, userRepository, passwordEncoder);

        // Act
        runner.run();

        // Assert
        verify(storeRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
