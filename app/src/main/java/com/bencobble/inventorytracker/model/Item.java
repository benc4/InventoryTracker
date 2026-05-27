package com.bencobble.inventorytracker.model;

import com.google.firebase.firestore.DocumentId;

// Item model class
// Represents an inventory item with unique id, name, optional description, and quantity
public class Item {

    /*Columns*/
    @DocumentId
    private String id;
    private String name;
    private String description;
    private int quantity;

    /*Constructors*/
    public Item() {} // No-argument constructor needed for Firebase

    public Item(String name, String description, int quantity) {
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    /*Getters and setters*/
    public String getID() {
        return id;
    }
    public void setID(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
