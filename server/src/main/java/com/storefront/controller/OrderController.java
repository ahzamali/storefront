package com.storefront.controller;

import com.storefront.dto.OrderRequestDTO;
import com.storefront.model.AppUser;
import com.storefront.model.CustomerOrder;
import com.storefront.repository.AppUserRepository;
import com.storefront.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final AppUserRepository userRepository;

    public OrderController(OrderService orderService, AppUserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        CustomerOrder order = orderService.createOrder(request, user);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        java.util.List<Long> storeIdsParam = null;

        // RBAC Logic
        if (user.getRole() == com.storefront.model.Role.SUPER_ADMIN
                || user.getRole() == com.storefront.model.Role.ADMIN) {
            // Admin can see all, or filter by specific store if requested
            if (storeId != null) {
                storeIdsParam = java.util.Collections.singletonList(storeId);
            }
        } else {
            // Non-admin: Get assigned stores
            java.util.List<Long> allowedStoreIds = user.getStores().stream()
                    .map(com.storefront.model.Store::getId)
                    .collect(java.util.stream.Collectors.toList());

            if (allowedStoreIds.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }

            if (storeId != null) {
                // User wants specific store. Check if allowed.
                if (!allowedStoreIds.contains(storeId)) {
                    // Unauthorized to see this specific store
                    // Return empty or error? Empty is safer/standard for search filters
                    return ResponseEntity.ok(java.util.Collections.emptyList());
                }
                storeIdsParam = java.util.Collections.singletonList(storeId);
            } else {
                // No specific store requested, return orders from all assigned stores
                storeIdsParam = allowedStoreIds;
            }
        }

        return ResponseEntity.ok(orderService.searchOrders(customerName, customerPhone, storeIdsParam));
    }
}
