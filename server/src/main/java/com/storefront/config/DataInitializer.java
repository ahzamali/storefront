package com.storefront.config;

import com.storefront.model.Store;
import com.storefront.repository.StoreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(StoreRepository storeRepository,
            com.storefront.repository.AppUserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            if (storeRepository.findFirstByType(Store.StoreType.MASTER).isEmpty()) {
                Store masterStore = new Store("Master Store", Store.StoreType.MASTER, null);
                storeRepository.save(masterStore);
            }

            if (userRepository.findByUsername("superadmin").isEmpty()) {
                com.storefront.model.AppUser admin = new com.storefront.model.AppUser("superadmin",
                        passwordEncoder.encode("password"), com.storefront.model.Role.SUPER_ADMIN);
                userRepository.save(admin);
            }
        };
    }
}
