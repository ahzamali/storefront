package com.storefront.controller;

import com.storefront.dto.BundleDTO;
import com.storefront.dto.StockIngestDTO;
import com.storefront.model.Product;
import com.storefront.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(inventoryService.createProduct(product));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }

    @GetMapping("/view")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<?> getInventoryView(@RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(inventoryService.getInventoryView(storeId));
    }

    @PostMapping("/bundles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> createBundle(@RequestBody BundleDTO bundleDTO) {
        return ResponseEntity.ok(inventoryService.createBundle(bundleDTO));
    }

    @GetMapping("/bundles")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<List<com.storefront.dto.BundleViewDTO>> getAllBundles() {
        return ResponseEntity.ok(inventoryService.getAllBundles());
    }

    @PostMapping("/stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> addStock(@RequestBody StockIngestDTO dto) {
        return ResponseEntity.ok(inventoryService.addStock(dto.getSku(), dto.getQuantity()));
    }

    @PostMapping("/ingest/isbn")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> ingestIsbn(@RequestBody java.util.Map<String, Object> payload) {
        String isbn = (String) payload.get("isbn");
        int quantity = (int) payload.getOrDefault("quantity", 1);
        return ResponseEntity.ok(inventoryService.ingestBook(isbn, quantity));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(inventoryService.updateProduct(id, product));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> updateStockCount(@RequestBody com.storefront.dto.StockUpdateDTO dto) {
        return ResponseEntity.ok(inventoryService.updateStockCount(dto.getSku(), dto.getQuantity(), dto.getStoreId()));
    }
}
