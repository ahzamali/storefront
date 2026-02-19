# Test Gap Implementation Plan

## Summary
**Total Tests Identified**: 166
**Currently Implemented**: 91 (55%)
**Remaining**: 75 tests needed

---

## Test Breakdown by Category

### ✅ Fully Covered (91 tests)
1. **Unit Tests**: 31/31 (100%)
2. **Integration Tests**: 27/27 (100%)
3. **Validation Tests**: 22/22 (100%)
4. **Security Tests**: 11/11 (100%)

### ❌ Critical Gaps (53 tests needed)

#### 1. Input Validation (35 tests)
**Priority**: HIGH
**Effort**: 2-3 days

| Subcategory | Tests Needed | Current | Gap |
|-------------|--------------|---------|-----|
| Authentication | 12 | 0 | 12 |
| Product/Inventory | 8 | 0 | 8 |
| Order | 6 | 3 | 3 |
| Store/Allocation | 5 | 0 | 5 |
| **Subtotal** | **31** | **3** | **28** |

**Key Tests**:
- Null/empty username, password validation
- Password strength requirements
- SKU, name, price validation
- Quantity validation (negative, zero)
- Duplicate prevention (username, SKU)

#### 2. Password Security (9 tests)
**Priority**: HIGH
**Effort**: 1-2 days

- Weak password detection (no uppercase, lowercase, digit, special char)
- Common password prevention
- Password reuse prevention
- Rate limiting on failed logins
- Account lockout mechanism

#### 3. Data Integrity (9 tests)
**Priority**: HIGH
**Effort**: 2 days

- Duplicate prevention (username, SKU)
- Cascade delete verification
- Orphaned record prevention
- Referential integrity checks
- Transaction rollback verification
- Concurrent operation handling

### ⚠️ Important Gaps (22 tests needed)

#### 4. Error Handling (10 tests)
**Priority**: MEDIUM
**Effort**: 2 days

- Database connection failures
- External API failures (Google Books)
- Malformed JSON handling
- Large payload handling
- Timeout scenarios

#### 5. Business Logic Edge Cases (7 tests)
**Priority**: MEDIUM
**Effort**: 1 day

- Bundle with deleted products
- Store deletion with orders
- User deletion with stores
- Reactivate deleted products
- Double reconciliation

#### 6. API Security (8 tests)
**Priority**: MEDIUM
**Effort**: 2 days

- XSS prevention
- Path traversal prevention
- CSRF protection
- Rate limiting
- CORS validation

---

## Implementation Roadmap

### Sprint 1: Critical Validation (Week 1-2)
**Goal**: Prevent invalid data from entering the system

**Tasks**:
1. Add validation annotations to all DTOs
   - `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`
2. Implement password strength validator
3. Add duplicate prevention tests
4. Write 35 input validation tests

**Deliverables**:
- All DTOs have validation annotations
- Password strength enforced
- 35 new tests passing

### Sprint 2: Data Integrity & Security (Week 3-4)
**Goal**: Ensure data consistency and security

**Tasks**:
1. Implement rate limiting on login
2. Add account lockout mechanism
3. Write data integrity tests (9 tests)
4. Write password security tests (9 tests)
5. Add transaction rollback tests

**Deliverables**:
- Rate limiting active
- Account lockout working
- 18 new tests passing

### Sprint 3: Error Handling (Week 5)
**Goal**: Graceful failure handling

**Tasks**:
1. Add global exception handler improvements
2. Implement circuit breaker for external APIs
3. Add request size limits
4. Write error handling tests (10 tests)

**Deliverables**:
- Robust error handling
- 10 new tests passing

### Sprint 4: Edge Cases & API Security (Week 6)
**Goal**: Complete test coverage

**Tasks**:
1. Write business logic edge case tests (7 tests)
2. Implement XSS prevention
3. Add CSRF protection
4. Write API security tests (8 tests)

**Deliverables**:
- 15 new tests passing
- **100% test coverage achieved**

---

## Code Changes Required

### 1. DTO Validation (High Priority)
```java
// RegisterRequestDTO.java
public class RegisterRequestDTO {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @StrongPassword
    private String password;
    
    @NotNull(message = "Role is required")
    private String role;
}

// OrderRequestDTO.java
public class OrderRequestDTO {
    @NotNull(message = "Store ID is required")
    private Long storeId;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequestDTO> items;
}

// OrderItemRequestDTO.java
public class OrderItemRequestDTO {
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
```

### 2. Password Validator (High Priority)
```java
@Component
public class PasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;
        
        return password.length() >= 8
            && password.matches(".*[A-Z].*")
            && password.matches(".*[a-z].*")
            && password.matches(".*\\d.*")
            && password.matches(".*[@#$%^&+=!].*");
    }
}
```

### 3. Rate Limiting (High Priority)
```java
@Component
public class LoginAttemptService {
    private final LoadingCache<String, Integer> attemptsCache;
    
    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Integer>() {
                public Integer load(String key) {
                    return 0;
                }
            });
    }
    
    public void loginFailed(String username) {
        int attempts = attemptsCache.getUnchecked(username);
        attemptsCache.put(username, attempts + 1);
    }
    
    public boolean isBlocked(String username) {
        return attemptsCache.getUnchecked(username) >= 5;
    }
}
```

### 4. Global Exception Handler Enhancement (Medium Priority)
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleValidationExceptions(
        MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });
    return ResponseEntity.badRequest().body(errors);
}

@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Map<String, String>> handleDuplicateKey(
        DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "Duplicate entry. Resource already exists."));
}
```

---

## Testing Strategy

### Unit Tests
- Mock all dependencies
- Test individual methods
- Fast execution (< 1 second per test)

### Integration Tests
- Use H2 in-memory database
- Test full workflows
- Verify database state

### Validation Tests
- Test boundary conditions
- Test null/empty inputs
- Test invalid formats

### Security Tests
- Test authentication flows
- Test authorization rules
- Test injection prevention

---

## Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Total Tests | 91 | 166 |
| Test Coverage | 55% | 100% |
| Code Coverage | ~80% | >90% |
| Critical Bugs | Unknown | 0 |
| Security Vulnerabilities | Unknown | 0 |

---

## Dependencies Required

```xml
<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>31.1-jre</version>
</dependency>

<!-- Circuit Breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>2.0.2</version>
</dependency>
```

---

## Conclusion

This plan provides a clear roadmap to achieve **100% test coverage** in 6 weeks. The focus is on:
1. **Preventing invalid data** (Sprint 1)
2. **Ensuring security** (Sprint 2)
3. **Handling errors gracefully** (Sprint 3)
4. **Covering edge cases** (Sprint 4)

All test specifications are documented in `spec/ServerTests.md` for implementation reference.
