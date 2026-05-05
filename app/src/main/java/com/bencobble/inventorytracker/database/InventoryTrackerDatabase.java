package com.bencobble.inventorytracker.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.bencobble.inventorytracker.model.User;
import com.bencobble.inventorytracker.model.Item;

// Sets up Room database
@Database(entities = {User.class, Item.class}, version = 1)
public abstract class InventoryTrackerDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract ItemDao itemDao();
}
