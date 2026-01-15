# StoreFront v2 Project Specification

## 1. Project Overview
StoreFront v2 is a comprehensive inventory and sales management system designed for hybrid retail environments. It supports a central warehouse model ("Master Store") and ad-hoc point-of-sale locations ("Virtual Stores"). 
The system should be multi-tenant, meaning that it should be able to support multiple stores. With users limited to their own store.
There will be a super admin who can create stores and the super admin can add admin users for each store. The admin users of each store should be able add employees to their store.

The system consists of three main components:
1.  **Backend Server**: Central API and database host.
2.  **Web Dashboard**: Admin interface for inventory and store management.
3.  **Mobile App**: Point-of-sale application for employees.

> **Note**: For detailed technical architecture and component breakdown, please refer to the `SystemArchitecture.md` document.

## 2. Core Domain Concepts

### 2.1 Stores
-   **Inventory Location**: Abstract concept representing any place holding stock.
-   **Master Store**: The default physical warehouse. Automatically seeded on startup if missing.
    -   *Role*: Source of truth for all inventory.
-   **Virtual Stores**: Temporary or logical stores (e.g., a pop-up stall, a specific van) assigned to a user.
    -   *Lifecycle*: Created -> Allocated Stock -> Selling -> Reconciled -> Closed.

### 2.2 Inventory & Products
-   **Product**: Individual items with polymorphic attributes (e.g., `Book` with ISBN/Author, `Pencil` with Brand/Hardness).
-   **Bundle**: A collection of products sold as a single unit (e.g., "School Kit Grade 5").
    -   *Exclusions*: Ability to sell a bundle but remove specific items at POS (adjusting price/inventory accordingly).

### 2.3 User Roles
-   **SUPER_ADMIN**: System-wide control, creates stores and store admins.
-   **STORE_ADMIN**: Manages a specific store's inventory, users, and allocations.
-   **EMPLOYEE**: Point-of-sale access, assigned to specific Virtual Stores.

## 3. Implemented Features

### 3.1 System Utilities
-   **Data Persistence**: Database (H2) survives application restarts.
-   **Auto-Seeding**: "Master Store" and default users are automatically created on startup.
-   **H2 Console**: Accessible for direct database inspection.

### 3.2 Inventory Management
-   **Stock View**: Real-time view of Master Store and Virtual Store stock levels.
-   **Stock Ingestion**:
    -   **Web**: API-driven stock addition.
    -   **Mobile**: "Scan ISBN" (Google Books integration) and "Add Manually" (for non-book items).
-   **Product Management**: Support for creating and managing Products and Bundles.
-   **Polymorphism**: Backend and Database support for different product types (`Book`, `Stationery`).

### 3.3 Store Operations
-   **Allocation Logic**: Moving stock from Master Store to Virtual Stores.
-   **Reconciliation**: Closing out a Virtual Store, counting returns, and generating discrepancy reports.
-   **Barcode Scanning**: Mobile app integration for finding products and adding to cart.

### 3.4 Security
-   **Role-Based Access**: Strict separation of concerns (Admin vs Employee).
-   **JWT Authentication**: Secure, stateless API access.

## 4. Future Roadmap
-   **Sales Reporting**: Aggregated reports from Virtual Store sales (Advanced analytics).
-   **Multi-tenant Isolation**: Stronger data isolation between different tenant stores.
-   **Offline Sync**: Enhanced offline capabilities for the mobile app.
-   **SSO Integration**: Enable Single Sign-On (SSO) authentication for users.
-   **Product Images**: Ability to upload and display images for products.

