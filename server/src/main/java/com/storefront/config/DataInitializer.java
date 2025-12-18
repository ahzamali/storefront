package com.storefront.config;

import com.storefront.model.Store;
import com.storefront.repository.StoreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(StoreRepository storeRepository) {
        return args -> {
            if (storeRepository.findFirstByType(Store.StoreType.MASTER).isEmpty()) {
                Store masterStore = new Store("Master Store", Store.StoreType.MASTER, null);
                storeRepository.save(masterStore);
            }
        };
    }
}
