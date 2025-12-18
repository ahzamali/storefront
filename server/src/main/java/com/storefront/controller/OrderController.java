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
}
