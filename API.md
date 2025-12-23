# StoreFront v2 API Documentation

This document lists all the REST APIs available in the StoreFront v2 application.

## Base URL
`http://localhost:8080/api/v1`

## Authentication (`/auth`)

| Method | Endpoint | Description | Roles | Request Body / Params |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/login` | Authenticate user and get token. | Public | `{ "username": "...", "password": "..." }` |
| `POST` | `/register` | Register a new user. | Super Admin, Store Admin | `{ "username": "...", "password": "...", "role": "...", "storeId": "..." }` |
| `GET` | `/users` | List all registered users. | Super Admin, Store Admin | - |
| `DELETE` | `/users/{id}` | Delete a user by ID. | Admin, Super Admin | - |

## Inventory (Global/HQ) (`/inventory`)

| Method | Endpoint | Description | Roles | Request Body / Params |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/products` | Create a new product. | Admin | `{ "sku": "...", "name": "...", "basePrice": ..., "type": "..." }` |
| `GET` | `/products` | List all global products. | Public | - |
| `GET` | `/view` | Get aggregated inventory view. | Public | Response: `[ { "id": 1, "sku": "...", "name": "...", "type": "...", "basePrice": ..., "quantity": ..., "attributes": { ... } } ]` |
| `POST` | `/bundles` | Create a product bundle. | Admin | `{ "sku": "...", "name": "...", "items": [{ "productSku": "...", "quantity": ... }] }` |
| `POST` | `/stock` | Add stock to Master Store. | Admin | `{ "sku": "...", "quantity": ... }` |
| `POST` | `/ingest/isbn` | Ingest book details & stock via ISBN. | Admin, Super Admin | `{ "isbn": "...", "quantity": ... }` |

## Stores (`/stores`)

| Method | Endpoint | Description | Roles | Request Body / Params |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/` | List all stores. | Public | - |
| `POST` | `/` | Create a new store (Virtual/Physical). | Admin | `{ "name": "..." }` |
| `POST` | `/{id}/allocate` | Allocate stock from Master to Store. | Admin | `{ "items": [{ "sku": "...", "quantity": ... }] }` |
| `POST` | `/{id}/reconcile` | Reconcile store inventory (sync). | Admin | - |
| `GET` | `/{storeId}/inventory` | Search inventory in a specific store. | Public | Query: `search` (Matches Name or ISBN in attributes) |

## Orders (`/orders`)

| Method | Endpoint | Description | Roles | Request Body / Params |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/` | Create a new customer order. | Public | `{ "storeId": ..., "customerName": "...", "customerPhone": "...", "items": [...] }` |
| `GET` | `/` | Search orders by customer. | Public | Query: `customerName`, `customerPhone`. Results sorted by `createdAt` DESC (Newest first). |

---
**Note**: All endpoints requiring Roles must include the `Authorization: Bearer <token>` header.
