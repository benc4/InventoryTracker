package com.bencobble.inventorytracker.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


// Represents a User
// Uses Room to create a table
@Entity
public class User {

    /*Columns*/
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    private long mID;

    @NonNull
    @ColumnInfo(name = "username")
    private final String mUsername;

    @NonNull
    @ColumnInfo(name = "password")
    private final String mPassword;

    /*Constructor*/
    public User(@NonNull String username, @NonNull String password) {
        mUsername = username;
        mPassword = password;
    }

    /*Getters and setters*/
    public long getID() {
        return mID;
    }
    public void setID(long id) {
        mID = id;
    }

    @NonNull
    public String getUsername() {
        return mUsername;
    }

    @NonNull
    public String getPassword() {
        return mPassword;
    }
}
