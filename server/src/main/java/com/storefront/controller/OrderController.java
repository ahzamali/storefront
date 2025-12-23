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
            @AuthenticationPrincipal UserDetails userDetails) {

        AppUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        java.util.List<Long> storeIds = null;

        // If not SUPER_ADMIN, filter by assigned stores
        if (user.getRole() != com.storefront.model.Role.SUPER_ADMIN
                && user.getRole() != com.storefront.model.Role.ADMIN) {
            storeIds = user.getStores().stream()
                    .map(com.storefront.model.Store::getId)
                    .collect(java.util.stream.Collectors.toList());

            // If non-admin user has no assigned stores, they see nothing (or empty list)
            if (storeIds.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
        }

        return ResponseEntity.ok(orderService.searchOrders(customerName, customerPhone, storeIds));
    }
}
