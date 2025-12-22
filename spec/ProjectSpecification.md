# StoreFront v2 Project Specification

## 1. Project Overview
StoreFront v2 is a comprehensive inventory and sales management system designed for hybrid retail environments. It supports a central warehouse model ("Master Store") and ad-hoc point-of-sale locations ("Virtual Stores"). 
The system should be multi-tenant, meaning that it should be able to support multiple stores. With users limited to their own store.
There will be a super admin who can create stores and the super admin can add admin users for each store. The admin users of each store should be able add employees to their store.

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
-   **User Management**: Add user management functionality 
-   **Store Management**: Add store management functionality
-   **Role Management**: Add role management functionality
-   **Permission Management**: Add permission management functionality
-   **Audit Logging**: Add audit logging functionality

### 2.3 Mobile (Android)
-   **Platform**: Native Android (Kotlin)
-   **Key Features**: Barcode scanning, Bundle sales, Stock reconciliation.
-   **User Management**: Add user management functionality 
-   **Store Management**: Add store management functionality
-   **Role Management**: Add role management functionality
-   **Permission Management**: Add permission management functionality
-   **Audit Logging**: Add audit logging functionality

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
-   **Stock View**: Real-time view of Master Store stock levels in the Web Dashboard. As well as there should be an option to select the store to view the inventory.
-   **Stock Ingestion**: API to add stock to the Master Store. This API should be accessible from the web dashboard. Given an ISBN the system should look up the details of the book, like the title, authors publishers etc. and add it to the Master Store.
-   **Product Management**: CRUD operations for Products and Bundles. This should be accessible from the web dashboard. We should be able to assign product bundles to each virtual store.
**Bundle Creation Workflow**: Create a workflow, where a new bundle can be created, starting with create bundle and then add each item from existing inventory or add new item by scanning a bar code or ISBN. 


### 4.3 Security
-   **Role-Based Access**:
    -   `ADMIN`: Full access to inventory and store management.
    -   `EMPLOYEE`: POS access.

## 5. Future Roadmap
-   **Allocation Logic**: Moving stock from Master to Virtual Stores.
-   **Sales Reporting**: Aggregated reports from Virtual Store sales.
-   **Barcode Scanning**: Mobile app integration for finding products.
