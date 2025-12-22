package com.storefront.controller;

import com.storefront.dto.AllocationRequestDTO;
import com.storefront.dto.AllocationRequestDTO;
import com.storefront.model.AppUser;
import com.storefront.model.Store;
import com.storefront.model.StockLevel;
import com.storefront.repository.AppUserRepository;
import com.storefront.service.InventoryService;
import com.storefront.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List; // Added import for List
import com.storefront.model.StockLevel; // Added import for StockLevel
import com.storefront.service.InventoryService; // Added import for InventoryService

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;
    private final AppUserRepository userRepository;
    private final InventoryService inventoryService;

    public StoreController(StoreService storeService, AppUserRepository userRepository,
            InventoryService inventoryService) {
        this.storeService = storeService;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> createStore(@RequestBody Map<String, String> body) {
        Store store = storeService.createStore(body.get("name"), Store.StoreType.VIRTUAL, null);
        return ResponseEntity.ok(store);
    }

    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> allocate(@PathVariable Long id, @RequestBody AllocationRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        storeService.allocateStock(id, request, user);
        return ResponseEntity.ok("Allocated");
    }

    @GetMapping("/{storeId}/inventory")
    public ResponseEntity<List<StockLevel>> getStoreInventory(@PathVariable Long storeId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(inventoryService.searchInventory(storeId, search));
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> reconcile(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        storeService.reconcileStore(id, user);
        return ResponseEntity.ok("Reconciled");
    }

    @GetMapping
    public java.util.List<Store> listStores() {
        return storeService.getAllStores();
    }
}
