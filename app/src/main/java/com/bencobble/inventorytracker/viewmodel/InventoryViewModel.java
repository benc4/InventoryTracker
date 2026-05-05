package com.bencobble.inventorytracker.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.List;

public class InventoryViewModel extends AndroidViewModel {
    // Result codes for Item CRUD operations
    public enum OperationResult {
        SUCCESS,
        SUCCESS_LOW_STOCK,
        SUCCESS_DELETE,
        ADD_FAILED,
        EMPTY_FIELDS,
        UPDATE_FAILED,
        CANNOT_DECREASE,
        DELETE_FAILED,
        GET_ITEMS_FAILED
    }

    private final InventoryRepository mRepo;

    // LiveData for all items in the database
    private final LiveData<List<Item>> mAllItems;

    // LiveData for the search query
    private final MutableLiveData<String> mSearchQuery = new MutableLiveData<>("");

    // LiveData for filtered items based on search query
    private final MediatorLiveData<List<Item>> mFilteredItems = new MediatorLiveData<>();

    // LiveData for the result of CRUD operations
    private final MutableLiveData<OperationResult> mOperationResult = new MutableLiveData<>();
    public LiveData<OperationResult> getOperationResult() { return mOperationResult; }

    // LiveData for the name of an Item that is out of stock
    private final MutableLiveData<String> mLowStockItemName = new MutableLiveData<>();
    public LiveData<String> getLowStockItemName() { return mLowStockItemName; }

    public InventoryViewModel(Application application) {
        super(application);
        mRepo = InventoryRepository.getInstance(application.getApplicationContext());
        mAllItems = mRepo.getItems(mOperationResult); // Get all items from db

        // Set mFilteredItems to watch changes to items in the db and search query
        // Updates mFilteredItems with new list when either changes
        mFilteredItems.addSource(mAllItems, items -> updateFilter());
        mFilteredItems.addSource(mSearchQuery, query -> updateFilter());
    }

    // Returns all items in the db that match the search query (if there is one)
    public LiveData<List<Item>> getItems() {
        return mFilteredItems;
    }

    // Set search query using text from search bar
    // When there is no query (null), the empty string is used to return all items
    public void setSearchQuery(String query) {
        mSearchQuery.setValue(query == null ? "" : query);
    }

    // Calls filter() with updated values
    private void updateFilter() {
        // Get updated item list from db and current search query
        List<Item> items = mAllItems.getValue();
        String query = mSearchQuery.getValue();

        // No items in the db, no need to filter
        if (items == null) { return; }

        // Update mFilteredItems with new filtered list of items
        mFilteredItems.setValue(filter(items, query));
    }

    // Filter items based on search query
    private List<Item> filter(List<Item> items, String query) {
        // No query, return all items
        if (query == null || query.trim().isEmpty()) {
            return items;
        }

        // List to hold filtered items
        List<Item> filtered = new ArrayList<>();

        // Convert query to lowercase for consistency
        String lowerCaseQuery = query.trim().toLowerCase();

        // Check if each item name matches the query
        for (Item item : items) {
            if (item.getName().toLowerCase().contains(lowerCaseQuery)) { // Item matches query
                filtered.add(item);
            }
        }
        return filtered;
    }

    // Get item from db with its id
    public LiveData<Item> getItem(long id) {
        return mRepo.getItem(id);
    }

    // Add item to database
    public void addItem(String name, String description, String quantityStr) {
        // Don't allow empty name or quantity
        if (name.isEmpty() || quantityStr.isEmpty()) {
            mOperationResult.setValue(OperationResult.EMPTY_FIELDS);
            return;
        }

        // Convert quantityStr to int and add new Item to db
        int quantity = Integer.parseInt(quantityStr);
        Item newItem = new Item(name, description, quantity);
        mRepo.addItem(newItem, mOperationResult);
    }

    // Update item in database
    public void updateItem(Item item, String name, String description, String quantityStr) {
        // Don't allow empty name or quantity
        if (name.isEmpty() || quantityStr.isEmpty()) {
            mOperationResult.setValue(OperationResult.EMPTY_FIELDS);
            return;
        }

        // Convert quantityStr to int
        int quantity = Integer.parseInt(quantityStr);

        // Update Item object with new values
        item.setName(name);
        item.setDescription(description);
        item.setQuantity(quantity);

        // Update item in db
        mRepo.updateItem(item, mOperationResult);
    }

    // Increase quantity of an item by 1
    public void increaseQuantity(Item item) {
        item.setQuantity(item.getQuantity() + 1);
        mRepo.updateItem(item, mOperationResult);
    }

    // Decrease quantity of an item by 1
    // Only if it is greater than 0
    public void decreaseQuantity(Item item) {
        if (item.getQuantity() > 0) {
            item.setQuantity(item.getQuantity() - 1);
            mRepo.decreaseQuantity(item, mOperationResult, mLowStockItemName);
        } else {
            mOperationResult.setValue(OperationResult.CANNOT_DECREASE);
        }
    }

    // Clear item from mLowStockItemName
    public void resetLowStockItemName() {
        mLowStockItemName.setValue(null);
    }

    // Delete item from database
    public void deleteItem(Item item) {
        mRepo.deleteItem(item, mOperationResult);
    }
}
