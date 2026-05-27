package com.bencobble.inventorytracker.model;

import java.util.Date;

// AuditLog model class
// Represents an audit log entry with unique id, timestamp, user id, item id, action, and details
public class AuditLog {

    /*Columns*/
    private Date timestamp;
    private String userId;
    private String itemId;
    private String action;
    private String details;

    /*Constructors*/
    public AuditLog() {} // No-argument constructor needed for Firebase

    public AuditLog(String userId, String itemId, String action, String details) {
        this.timestamp = new Date();
        this.userId = userId;
        this.itemId = itemId;
        this.action = action;
        this.details = details;
    }

    /*Getters and setters*/
    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getItemId() {
        return itemId;
    }
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
}
