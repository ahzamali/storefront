# Prompts

This document contains the prompts used in the application or relevant for the project's development.

## Template

**Purpose:** [Describe what this prompt achieves]
**Context:** [When is this prompt used?]
**Variables:**
- `{{variable_name}}`: [Description of variable]

**Content:**
```text
[Insert Prompt Here]
```

Product Modeling:

The Product definition is generic which is good, how ever it fails to capture other attribute of products, such as description, data of publication, author name etc. 
These attributes are very specific to book product type, if we generalize we may need different attributes for different product types. What would be the best way to create these attributes for examples, pecil may have lead thickness and darkness rating. 
Let us model the objects to capture these aspects of the products

Order Creation

We need to keep track of customers as well, lets as a way to record customer Name and Phone number with the orders.
We should be able to list all orders and filter them by customer name or phone numbers

Inventory Management:

We need to be able to view inventory for selected virtual store. We should also be able to search on the inventory with name of the product, ISBN in case of books, 

User Management

Lets move one by one, in the frontend we do not have a complete user management functionality, On the Admin Panel lets add user management. where we can create users with different roles, there should be a way to create users with access only to virtual stores and users having access to both Master and Virtual stores. 

The user management UI does not list the available store while creating a store admin for a store scope

Lets also add a functionality to delete users in the front end


Sale Order
lets add order processing, in the frontend we need to create a flow to start a new sale 
- start sale, 
- select customer - name, number (optional) 
- select items that should need to be sold, when a bundle is selected each item from the bundle loads up individually. 
- show total prices 
- go to a verification screen on submit 
- confirm sale on this screen
- each sale order should be registered agaist a store_Id, which will help us reconcile the inventory