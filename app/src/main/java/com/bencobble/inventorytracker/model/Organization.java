package com.bencobble.inventorytracker.model;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

// Organization model class
// Represents an organization of users that share the same items
public class Organization {

    /*Columns*/
    @DocumentId
    private String id;
    private String name;
    private String createdBy;
    private Date createdAt;
    private String inviteCode;

    /*Constructors*/
    public Organization() {} // No-argument constructor needed for Firebase

    public Organization(String name, String createdBy, String inviteCode) {
        this.name = name;
        this.createdBy = createdBy;
        this.inviteCode = inviteCode;
        this.createdAt = new Date();
    }

    /*Getters and setters*/
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getInviteCode() {
        return inviteCode;
    }
    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
