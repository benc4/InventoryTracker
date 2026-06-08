package com.bencobble.inventorytracker.model;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

// Item model class
// Represents an inventory item with unique id, name, optional description, and quantity
public class Item {

    /*Columns*/
    @DocumentId
    private String id;
    private String name;
    private String description;
    private int quantity;
    private List<String> searchTokens; // Prefix tokens of the name for search filtering

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

    public List<String> getSearchTokens() {
        return searchTokens;
    }
    public void setSearchTokens(List<String> searchTokens) {
        this.searchTokens = searchTokens;
    }
}
