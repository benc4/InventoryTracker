package com.bencobble.inventorytracker.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Represents an Item
// Uses Room to create a table
@Entity
public class Item {

    /*Columns*/
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    private long mID;

    @NonNull
    @ColumnInfo(name = "name")
    private String mName;

    @ColumnInfo(name = "description")
    private String mDescription;

    @ColumnInfo(name = "quantity")
    private int mQuantity;

    /*Constructor*/
    public Item (@NonNull String name, String description, int quantity) {
        mName = name;
        mDescription = description;
        mQuantity = quantity;
    }

    /*Getters and setters*/
    public long getID() {
        return mID;
    }
    public void setID(long id) {
        mID = id;
    }

    @NonNull
    public String getName() {
        return mName;
    }
    public void setName(@NonNull String name) {
        mName = name;
    }

    public String getDescription() {
        return mDescription;
    }
    public void setDescription(String description) {
        mDescription = description;
    }

    public int getQuantity() {
        return mQuantity;
    }
    public void setQuantity(int quantity) {
        mQuantity = quantity;
    }
}
