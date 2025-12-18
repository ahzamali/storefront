package com.storefront.controller;

import com.storefront.dto.AllocationRequestDTO;
import com.storefront.model.AppUser;
import com.storefront.model.Store;
import com.storefront.repository.AppUserRepository;
import com.storefront.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;
    private final AppUserRepository userRepository;

    public StoreController(StoreService storeService, AppUserRepository userRepository) {
        this.storeService = storeService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createStore(@RequestBody Map<String, String> body) {
        Store store = storeService.createStore(body.get("name"), Store.StoreType.VIRTUAL, null);
        return ResponseEntity.ok(store);
    }

    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> allocate(@PathVariable Long id, @RequestBody AllocationRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        storeService.allocateStock(id, request, user);
        return ResponseEntity.ok("Allocated");
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
