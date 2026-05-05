package com.bencobble.inventorytracker.database;

import androidx.room.*;
import com.bencobble.inventorytracker.model.User;

// DAO for User
// Handles database operations for User
@Dao
public interface UserDao {
    // Find User from db by username
    @Query("SELECT * FROM User WHERE username = :username")
    User getUser(String username);

    // Add User, replace if its ID exists (not possible)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addUser(User user);
}
