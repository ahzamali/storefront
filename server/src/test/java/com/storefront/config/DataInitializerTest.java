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
    void initDatabase_shouldCreateMasterStore_whenItDoesNotExist() throws Exception {
        // Arrange
        StoreRepository storeRepository = mock(StoreRepository.class);
        when(storeRepository.findFirstByType(Store.StoreType.MASTER)).thenReturn(Optional.empty());

        DataInitializer dataInitializer = new DataInitializer();
        CommandLineRunner runner = dataInitializer.initDatabase(storeRepository);

        // Act
        runner.run();

        // Assert
        ArgumentCaptor<Store> storeCaptor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(storeCaptor.capture());
        Store savedStore = storeCaptor.getValue();
        assertEquals("Master Store", savedStore.getName());
        assertEquals(Store.StoreType.MASTER, savedStore.getType());
    }

    @Test
    void initDatabase_shouldNotCreateMasterStore_whenItAlreadyExists() throws Exception {
        // Arrange
        StoreRepository storeRepository = mock(StoreRepository.class);
        when(storeRepository.findFirstByType(Store.StoreType.MASTER)).thenReturn(Optional.of(new Store()));

        DataInitializer dataInitializer = new DataInitializer();
        CommandLineRunner runner = dataInitializer.initDatabase(storeRepository);

        // Act
        runner.run();

        // Assert
        verify(storeRepository, never()).save(any());
    }
}
