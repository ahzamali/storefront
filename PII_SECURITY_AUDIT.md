# PII & Security Exposure Audit Report
**Project:** StoreFront-v2  
**Date:** 2026-09-26  
**Status:** Action Required  

---

## 1. Executive Summary

A comprehensive security and data privacy assessment of the StoreFront-v2 repository (Spring Boot Server, Android Client, and React Web Application) was conducted to evaluate the storage, transmission, logging, and access control of **Personally Identifiable Information (PII)** and associated authentication credentials.

Several critical security vulnerabilities and data leakage pathways were identified, ranging from cleartext password and auth token logging to unauthenticated database console access and unencrypted mobile network transmission.

---

## 2. Findings Matrix

| ID | Finding | Category | Severity | File / Location |
| :--- | :--- | :--- | :--- | :--- |
| **SEC-01** | Plaintext Passwords & BCrypt Hashes Dumped to Console | Logging & Monitoring | **CRITICAL** | `server/src/main/java/com/storefront/service/AuthService.java:51-66` |
| **SEC-02** | Active JWT Auth Tokens & User Objects Printed to Logs | Logging & Monitoring | **CRITICAL** | `server/src/main/java/com/storefront/controller/AuthController.java:33-53` |
| **SEC-03** | Publicly Accessible H2 Database Console (`permitAll`) | Authentication & DB | **CRITICAL** | `server/src/main/java/com/storefront/config/SecurityConfig.java:37`, `application.properties:10` |
| **SEC-04** | Cleartext HTTP Traffic Enabled on Android Client | Network Security | **HIGH** | `android/app/src/main/AndroidManifest.xml:13` |
| **SEC-05** | Overly Permissive CORS with Allowed Credentials (`*`) | Network & API | **HIGH** | `server/src/main/java/com/storefront/config/SecurityConfig.java:76-80` |
| **SEC-06** | Full Customer Phone Numbers & Names Exposed in APIs | Data Minimization | **MEDIUM** | `server/src/main/java/com/storefront/service/OrderService.java:145-169`, `CustomerOrder.java:30-32` |
| **SEC-07** | Insecure SharedPreferences & Android Cloud Backup Enabled | Storage Security | **MEDIUM** | `android/app/src/main/java/com/storefront/app/ConfigManager.kt:7-46`, `AndroidManifest.xml:8` |
| **SEC-08** | Plaintext JWT Tokens Stored in Web `localStorage` | Client Security | **MEDIUM** | `web/src/services/api.js:35-38` |
| **SEC-09** | Default Superadmin Password Seeded on Startup | Configuration | **LOW** | `server/src/main/java/com/storefront/config/DataInitializer.java:23-25` |

---

## 3. Detailed Vulnerability Descriptions

### SEC-01: Plaintext Passwords and Hashes Written to Console Logs
- **Severity:** `CRITICAL`
- **Location:** `server/src/main/java/com/storefront/service/AuthService.java` (Lines 51–66)
- **Vulnerability:**
  During the authentication flow, raw debug statements output the user's plaintext password input and the database password hash directly to standard output:
  ```java
  System.out.println("=== LOGIN DEBUG ===");
  System.out.println("Attempting login for username: " + username);
  System.out.println("User found! Stored hash: " + user.getPasswordHash());
  System.out.println("Input password: " + password);
  System.out.println("Password matches: " + matches);
  ```
- **Impact:** Any user or log aggregator with access to standard output/container logs can read cleartext user passwords.

---

### SEC-02: Active JWT Auth Tokens Dumped in Logs
- **Severity:** `CRITICAL`
- **Location:** `server/src/main/java/com/storefront/controller/AuthController.java` (Lines 33–53)
- **Vulnerability:**
  Active JWT session bearer tokens and the entire login response object are logged:
  ```java
  System.out.println("Token generated: " + token);
  ...
  System.out.println("Returning response: " + response);
  ```
- **Impact:** Hijacking of active employee and administrator sessions directly from server logs without needing password cracking.

---

### SEC-03: Publicly Accessible H2 Database Console
- **Severity:** `CRITICAL`
- **Location:** `server/src/main/java/com/storefront/config/SecurityConfig.java` (Line 37), `server/src/main/resources/application.properties` (Line 10)
- **Vulnerability:**
  `SecurityConfig` explicitly whitelists `/h2-console/**` without authentication:
  ```java
  .requestMatchers("/api/v1/auth/**", "/h2-console/**").permitAll()
  ```
  Additionally, `spring.h2.console.enabled=true` is enabled, and `frameOptions` are disabled.
- **Impact:** Unauthenticated remote attackers can browse to `/h2-console`, execute SQL queries, and exfiltrate all tables (`customer`, `customer_order`, `app_user`, etc.).

---

### SEC-04: Cleartext HTTP Traffic Enabled on Android
- **Severity:** `HIGH`
- **Location:** `android/app/src/main/AndroidManifest.xml` (Line 13)
- **Vulnerability:**
  The manifest enables `android:usesCleartextTraffic="true"`.
- **Impact:** Communication between the mobile app and server can fall back to unencrypted HTTP, exposing customer phone numbers, names, purchase items, and login credentials to local network packet sniffing (e.g., public Wi-Fi).

---

### SEC-05: Overly Permissive CORS with Allowed Credentials
- **Severity:** `HIGH`
- **Location:** `server/src/main/java/com/storefront/config/SecurityConfig.java` (Lines 76–80)
- **Vulnerability:**
  Wildcard origins are configured alongside `setAllowCredentials(true)`:
  ```java
  configuration.setAllowedOriginPatterns(java.util.List.of("*"));
  configuration.setAllowCredentials(true);
  ```
- **Impact:** Malicious third-party websites can issue cross-origin requests to exfiltrate customer order history and user data.

---

### SEC-06: Customer PII Overexposure in Orders API
- **Severity:** `MEDIUM`
- **Location:** `server/src/main/java/com/storefront/service/OrderService.java` (Lines 145–169), `Customer.java`, `CustomerOrder.java`
- **Vulnerability:**
  The `GET /api/v1/orders` endpoint returns entire `Customer` entities (`name`, `phone`) in plaintext without masking or granular role-based authorization.
- **Impact:** Every store staff member can view and scrape unmasked customer phone numbers and names across transactions.

---

### SEC-07: Insecure SharedPreferences & App Backup on Android
- **Severity:** `MEDIUM`
- **Location:** `android/app/src/main/java/com/storefront/app/ConfigManager.kt` (Lines 7–46), `android/app/src/main/AndroidManifest.xml` (Line 8)
- **Vulnerability:**
  - `ConfigManager` uses standard `MODE_PRIVATE` `SharedPreferences` without hardware-backed encryption for `auth_token`, `username`, `user_role`, and `user_id`.
  - `android:allowBackup="true"` is enabled.
- **Impact:** Local extraction of session tokens via ADB backup commands or compromised device storage.

---

### SEC-08: Web App Plaintext Token Storage
- **Severity:** `MEDIUM`
- **Location:** `web/src/services/api.js` (Lines 35–38)
- **Vulnerability:**
  JWT tokens are persisted in browser `localStorage`.
- **Impact:** Tokens are accessible to JavaScript and vulnerable to extraction if any Cross-Site Scripting (XSS) vulnerability is introduced.

---

### SEC-09: Hardcoded Default Superadmin Credentials
- **Severity:** `LOW`
- **Location:** `server/src/main/java/com/storefront/config/DataInitializer.java` (Lines 23–25)
- **Vulnerability:**
  The server auto-creates `superadmin` with password `password` if not present in the database.
- **Impact:** High risk of immediate compromise if deployed without immediately rotating the default superadmin credentials.

---

## 4. Mobile Device Permissions Audit

- **Permissions Declared:**
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
- **Audit Result:** **CLEAN ✅**  
  No device hardware permissions (e.g. `READ_SMS`, `RECEIVE_SMS`, `READ_CONTACTS`, `ACCESS_FINE_LOCATION`, `CAMERA`) are requested or accessed by the Android application code.

---

## 5. Remediation Roadmap

1. **Remove Console Logging:**
   - Delete all `System.out.println` and `System.err.println` occurrences in `AuthService.java` and `AuthController.java`.
2. **Harden Server Configuration:**
   - Set `spring.h2.console.enabled=false` in production properties.
   - Restrict `/h2-console/**` in `SecurityConfig.java` to `hasRole('SUPER_ADMIN')` or remove it entirely in production builds.
   - Replace `*` wildcard in CORS with explicit allowed origin whitelist.
3. **Secure Mobile Client:**
   - Set `android:usesCleartextTraffic="false"` and `android:allowBackup="false"` in `AndroidManifest.xml`.
   - Migrate `ConfigManager.kt` to use `EncryptedSharedPreferences` from `androidx.security.crypto`.
4. **Implement Data Masking:**
   - Introduce DTOs for `CustomerOrder` responses that mask phone numbers (e.g., `+91 98****1234`) for non-admin roles.
