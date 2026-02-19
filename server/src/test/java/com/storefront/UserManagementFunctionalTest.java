package com.storefront;

import com.storefront.model.AppUser;
import com.storefront.model.Product;
import com.storefront.model.Role;
import com.storefront.model.Store;
import com.storefront.repository.AppUserRepository;
import com.storefront.repository.ProductRepository;
import com.storefront.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserManagementFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setup() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        adminToken = loginResponse.split("\"token\":\"")[1].split("\"")[0];
    }

    @Test
    void testUpdateUserPassword() throws Exception {
        AppUser user = createUser("testuser", "oldpass", Role.EMPLOYEE);

        String updateJson = """
            {
                "password": "newpassword123"
            }
            """;

        mockMvc.perform(put("/api/v1/auth/users/" + user.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        AppUser updated = userRepository.findById(user.getId()).orElseThrow();
        assert passwordEncoder.matches("newpassword123", updated.getPasswordHash());
    }

    @Test
    void testUpdateUserStoreAssignment() throws Exception {
        Store store1 = createStore("Store 1");
        Store store2 = createStore("Store 2");
        AppUser user = createUser("storeuser", "pass123", Role.STORE_ADMIN);

        String updateJson = """
            {
                "storeIds": [%d, %d]
            }
            """.formatted(store1.getId(), store2.getId());

        mockMvc.perform(put("/api/v1/auth/users/" + user.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        AppUser updated = userRepository.findById(user.getId()).orElseThrow();
        assert updated.getStores().size() == 2;
    }

    @Test
    void testEmployeeCannotUpdateOtherUsers() throws Exception {
        AppUser employee = createUser("employee1", "pass123", Role.EMPLOYEE);
        AppUser otherUser = createUser("employee2", "pass456", Role.EMPLOYEE);

        String empLoginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"employee1\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        employeeToken = empLoginResponse.split("\"token\":\"")[1].split("\"")[0];

        String updateJson = """
            {
                "password": "hacked"
            }
            """;

        mockMvc.perform(put("/api/v1/auth/users/" + otherUser.getId())
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testEmployeeCanUpdateOwnPassword() throws Exception {
        AppUser employee = createUser("selfupdate", "oldpass", Role.EMPLOYEE);

        String empLoginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"selfupdate\",\"password\":\"oldpass\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        employeeToken = empLoginResponse.split("\"token\":\"")[1].split("\"")[0];

        String updateJson = """
            {
                "password": "newpass123"
            }
            """;

        mockMvc.perform(put("/api/v1/auth/users/" + employee.getId())
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        AppUser updated = userRepository.findById(employee.getId()).orElseThrow();
        assert passwordEncoder.matches("newpass123", updated.getPasswordHash());
    }

    @Test
    void testOnlySuperAdminCanChangeStoreAssignments() throws Exception {
        Store store = createStore("Test Store");
        AppUser storeAdmin = createUser("storeadmin", "pass123", Role.STORE_ADMIN);
        storeAdmin.addStore(store);
        userRepository.save(storeAdmin);

        String adminLoginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"storeadmin\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        String storeAdminToken = adminLoginResponse.split("\"token\":\"")[1].split("\"")[0];

        AppUser employee = createUser("emp", "pass", Role.EMPLOYEE);

        String updateJson = """
            {
                "storeIds": [%d]
            }
            """.formatted(store.getId());

        mockMvc.perform(put("/api/v1/auth/users/" + employee.getId())
                .header("Authorization", "Bearer " + storeAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isForbidden());
    }

    private AppUser createUser(String username, String password, Role role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    private Store createStore(String name) throws Exception {
        String json = """
            {
                "name": "%s"
            }
            """.formatted(name);

        String response = mockMvc.perform(post("/api/v1/stores")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long storeId = Long.parseLong(response.split("\"id\":")[1].split(",")[0]);
        return storeRepository.findById(storeId).orElseThrow();
    }

    private com.storefront.model.Product createProduct(String sku, String name, double price, String type) throws Exception {
        String json = """
            {
                "sku": "%s",
                "name": "%s",
                "basePrice": %.2f,
                "type": "%s"
            }
            """.formatted(sku, name, price, type);

        mockMvc.perform(post("/api/v1/inventory/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        return productRepository.findBySku(sku).orElseThrow();
    }

    private void addStock(String sku, int quantity) throws Exception {
        String json = """
            {
                "sku": "%s",
                "quantity": %d
            }
            """.formatted(sku, quantity);

        mockMvc.perform(post("/api/v1/inventory/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    private void allocateStock(Long storeId, String sku, int quantity) throws Exception {
        String json = """
            {
                "items": [{"sku": "%s", "quantity": %d}]
            }
            """.formatted(sku, quantity);

        mockMvc.perform(post("/api/v1/stores/" + storeId + "/allocate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }
}
