package com.bencobble.inventorytracker.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.bencobble.inventorytracker.model.Item;

import java.util.List;

// DAO for Item
// Handles database operations for Item
@Dao
public interface ItemDao {
    // Get all items as a LiveData list
    @Query("SELECT * FROM Item")
    LiveData<List<Item>> getItems();

    // Get item by ID
    @Query("SELECT * FROM Item WHERE item_id = :id")
    //Item getItem(long id);
    LiveData<Item> getItem(long id);

    // Add item to db
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addItem(Item item);

    //Update item
    @Update
    void updateItem(Item item);

    // Delete item
    @Delete
    void deleteItem(Item item);
}
