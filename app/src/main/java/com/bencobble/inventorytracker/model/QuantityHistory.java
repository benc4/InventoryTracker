package com.bencobble.inventorytracker.model;

import java.util.Date;

// QuantityHistory model class
// Represents an item's quantity history
// with timestamp, old quantity, new quantity, id of who changed it
public class QuantityHistory {

    /*Columns*/
    private Date timestamp;
    private int oldQuantity;
    private int newQuantity;
    private String changedBy;

    /*Constructors*/
    public QuantityHistory() {} // No-argument constructor needed for Firebase

    public QuantityHistory(int oldQuantity, int newQuantity, String changedBy) {
        this.timestamp = new Date();
        this.oldQuantity = oldQuantity;
        this.newQuantity = newQuantity;
        this.changedBy = changedBy;
    }

    /*Getters and setters*/
    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getOldQuantity() {
        return oldQuantity;
    }
    public void setOldQuantity(int oldQuantity) {
        this.oldQuantity = oldQuantity;
    }

    public int getNewQuantity() {
        return newQuantity;
    }
    public void setNewQuantity(int newQuantity) {
        this.newQuantity = newQuantity;
    }

    public String getChangedBy() {
        return changedBy;
    }
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
