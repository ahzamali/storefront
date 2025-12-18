# StoreFront v2 Project Specification

## 1. Project Overview
StoreFront v2 is a comprehensive inventory and sales management system designed for hybrid retail environments. It supports a central warehouse model ("Master Store") and ad-hoc point-of-sale locations ("Virtual Stores").

The system consists of three main components:
1.  **Backend Server**: Central API and database host (Spring Boot).
2.  **Web Dashboard**: Admin interface for inventory and store management (React).
3.  **Mobile App**: Point-of-sale application for employees (Android).

## 2. Technical Architecture

### 2.1 Backend (Server)
-   **Framework**: Spring Boot 3.x (Java 17+)
-   **Database**: H2 Database (File-based persistence)
    -   *Location*: `./server/data/storefront`
    -   *Console*: `/h2-console` (Enabled for development)
-   **Security**: JWT Authentication (Stateless)
-   **API Design**: RESTful v1 API

### 2.2 Frontend (Web)
-   **Framework**: React (Vite)
-   **Access**: Headquarters / Admin users
-   **Key Features**: Master Inventory Management, Store Allocation.

### 2.3 Mobile (Android)
-   **Platform**: Native Android (Kotlin)
-   **Key Features**: Barcode scanning, Bundle sales, Stock reconciliation.

## 3. Core Domain Concepts

### 3.1 Stores
-   **Master Store**: The default physical warehouse. Automatically seeded on startup if missing.
    -   *Role*: Source of truth for all inventory.
-   **Virtual Stores**: Temporary or logical stores (e.g., a pop-up stall, a specific van).
    -   *Lifecycle*: Created -> Allocated Stock -> Selling -> Reconciled -> Closed.

### 3.2 Inventory & Products
-   **Product**: Individual items (Books, Stationery).
-   **Bundle**: A collection of products sold as a single unit (e.g., "School Kit Grade 5").
    -   *Exclusions*: Ability to sell a bundle but remove specific items at POS (adjusting price/inventory accordingly).

## 4. Implemented Features

### 4.1 System Utilities
-   **Data Persistence**: Database survives application restarts.
-   **Auto-Seeding**: "Master Store" is automatically created by `DataInitializer` on startup.
-   **H2 Console**: Accessible for direct database inspection.

### 4.2 Inventory Management
-   **Stock View**: Real-time view of Master Store stock levels in the Web Dashboard.
-   **Stock Ingestion**: API to add stock to the Master Store.
-   **Product Management**: CRUD operations for Products and Bundles.

### 4.3 Security
-   **Role-Based Access**:
    -   `ADMIN`: Full access to inventory and store management.
    -   `EMPLOYEE`: POS access.

## 5. Future Roadmap
-   **Allocation Logic**: Moving stock from Master to Virtual Stores.
-   **Sales Reporting**: Aggregated reports from Virtual Store sales.
-   **Barcode Scanning**: Mobile app integration for finding products.
