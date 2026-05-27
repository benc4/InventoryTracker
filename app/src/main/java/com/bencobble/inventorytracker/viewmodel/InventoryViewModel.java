package com.bencobble.inventorytracker.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.model.QuantityHistory;
import com.bencobble.inventorytracker.repository.ItemRepository;
import com.bencobble.inventorytracker.util.ParseIntHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

// InventoryViewModel used by AddItem, ItemDetails, and InventoryGrid Fragments
// Connects to ItemRepository for item CRUD operations
// Also handles search filtering and low stock item notifications
// Exposes OperationResult MutableLiveData observed by the fragments
// Managed by Hilt for dependency injection
@HiltViewModel
public class InventoryViewModel extends ViewModel {
    private final ItemRepository mRepo;

    // OperationResult enum
    // Contains status codes for item operations
    public enum OperationResult {
        SUCCESS,
        SUCCESS_LOW_STOCK,
        SUCCESS_DELETE,
        ADD_FAILED,
        EMPTY_FIELDS,
        UPDATE_FAILED,
        CANNOT_DECREASE,
        DELETE_FAILED,
        GET_ITEMS_FAILED,
        GET_QUANTITY_HISTORY_FAILED,
        INVALID_QUANTITY
    }

    /* Item lists and search filtering LiveData */

    // Holds the entire list of items from the user's items collection
    private final LiveData<List<Item>> mAllItems;

    // Holds the search string currently in the SearchView bar (defaults to "")
    private final MutableLiveData<String> mSearchQuery = new MutableLiveData<>("");

    // Holds the filtered list of items based on the search query
    private final MediatorLiveData<List<Item>> mFilteredItems = new MediatorLiveData<>();

    /* OperationResult LiveData */

    // LiveData to hold the OperationResult enum status code from item operations
    private final MutableLiveData<OperationResult> mOperationResult = new MutableLiveData<>();
    public LiveData<OperationResult> getOperationResult() { return mOperationResult; }

    /* Low stock item LiveData */

    // Holds the name of the item that has been changed to low stock (0 quantity)
    private final MutableLiveData<String> mLowStockItemName = new MutableLiveData<>();
    public LiveData<String> getLowStockItemName() { return mLowStockItemName; }

    /* Constructor */
    @Inject
    public InventoryViewModel(ItemRepository repo) {
        mRepo = repo;
        mAllItems = mRepo.getItems(); // Get initial list of all items

        // Set up mFilteredItems MediatorLiveData with sources to observer
        // Calls updateFilter when either mAllItems or mSearchQuery changes
        mFilteredItems.addSource(mAllItems, items -> updateFilter());
        mFilteredItems.addSource(mSearchQuery, query -> updateFilter());
    }

    /* Item methods */

    // getItems method
    // Returns the current filtered list of items
    // Observed by InventoryGridFragment to populate the RecyclerView
    public LiveData<List<Item>> getItems() {
        return mFilteredItems;
    }

    // setSearchQuery method
    // Takes a search query string as a parameter to update mSearchQuery
    // Called by InventoryGridFragment when the query changes or search bar is collapsed
    // Observed by mFilteredItems to update the filtered list of items on change
    public void setSearchQuery(String query) {
        mSearchQuery.setValue(query == null ? "" : query); // Empty string if query is null
    }

    // updateFilter method
    // Calls filter to update mFilteredItems with the filtered list
    // using the current state of mAllItems and mSearchQuery
    // Called by mFilteredItems MediatorLiveData when mAllItems or mSearchQuery changes
    private void updateFilter() {
        List<Item> items = mAllItems.getValue();
        String query = mSearchQuery.getValue();

        if (items == null) { return; } // Skip if there are no items to filter

        mFilteredItems.setValue(filter(items, query)); // Calls filter method
    }

    // filter method
    // Takes a list of items and a search query string as parameters
    // Runs a case-insensitive search of item names against the search query
    // Returns the filtered list
    // Called by updateFilter
    private List<Item> filter(List<Item> items, String query) {
        // Skip filtering if query is null/empty
        if (query == null || query.trim().isEmpty()) {
            return items;
        }

        List<Item> filtered = new ArrayList<>();
        String lowerCaseQuery = query.trim().toLowerCase(); // Convert query to lowercase

        // Loops through the list of items and checks if the item name contains the query
        // Adds matches to the filtered list
        for (Item item : items) {
            if (item.getName() != null
                    && item.getName().toLowerCase().contains(lowerCaseQuery)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    // getItem method
    // Takes an item ID as a parameter
    // Returns the matching item from the user's items collection
    // Calls the ItemRepository getItem method
    public LiveData<Item> getItem(String id) {
        return mRepo.getItem(id);
    }

    // stopItemListener method
    // Stops the snapshot listener from getItem
    // Called when ItemDetailsFragment is destroyed
    public void stopItemListener() {
        mRepo.stopItemListener();
    }

    // getQuantityHistory method
    // Takes an item ID as a parameter
    // Returns the matching list of QuantityHistory objects from the user's items' history collection
    // Calls the ItemRepository getQuantityHistory method
    public LiveData<List<QuantityHistory>> getQuantityHistory(String itemId) {
        return mRepo.getQuantityHistory(itemId, mOperationResult);
    }

    // stopQuantityHistoryListener method
    // Stops the snapshot listener from getQuantityHistory
    // Called when ItemDetailsFragment is destroyed
    public void stopQuantityHistoryListener() {
        mRepo.stopQuantityHistoryListener();
    }

    // addItem method
    // Takes a name, description, and quantity string as parameters
    // Calls addItem method from ItemRepository to add the item
    // Posts EMPTY_FIELDS to OperationResult if name or quantity is empty,
    // other outcomes are posted by the addItem repo method
    public void addItem(String name, String description, String quantityStr) {
        // Empty check before calling addItem repository method
        if (name.isEmpty() || quantityStr.isEmpty()) {
            mOperationResult.setValue(OperationResult.EMPTY_FIELDS);
            return;
        }

        // Convert quantityStr to integer to match Item's quantity type
        int quantity = ParseIntHelper.parseQuantity(quantityStr);
        if (quantity == -1) {
            mOperationResult.setValue(OperationResult.INVALID_QUANTITY);
            return;
        }


        // Builds the new item object and calls addItem repo method
        Item newItem = new Item(name, description, quantity);
        mRepo.addItem(newItem, mOperationResult);
    }

    // updateItem method
    // Takes an item, name, description, and quantity string as parameters
    // Calls updateItem method from ItemRepository to update the item
    // Posts EMPTY_FIELDS to OperationResult if name or quantity is empty,
    // other outcomes are posted by the updateItem repo method
    public void updateItem(Item item, String name, String description, String quantityStr) {
        // Empty check before calling updateItem repository method
        if (name.isEmpty() || quantityStr.isEmpty()) {
            mOperationResult.setValue(OperationResult.EMPTY_FIELDS);
            return;
        }

        // Convert quantityStr to integer to match Item's quantity type
        int quantity = ParseIntHelper.parseQuantity(quantityStr);
        if (quantity == -1) {
            mOperationResult.setValue(OperationResult.INVALID_QUANTITY);
            return;
        }

        // Save old quantity before building the updated item object
        int oldQuantity = item.getQuantity();

        // Update the local item object with new values
        item.setName(name);
        item.setDescription(description);
        item.setQuantity(quantity);

        // Calls updateItem repo method
        mRepo.updateItem(item, oldQuantity, mOperationResult, mLowStockItemName);
    }

    // increaseQuantity method
    // Takes an item as a parameter
    // Increases the item's quantity by 1
    // Calls updateItem repository method to make the update
    // Called by plus button in InventoryGridFragment/InventoryItemAdapter
    // Repository method posts the result to OperationResult
    public void increaseQuantity(Item item) {
        int oldQuantity = item.getQuantity();
        Item updatedItem = new Item(item.getName(), item.getDescription(), oldQuantity + 1);
        updatedItem.setID(item.getID());
        mRepo.updateItem(updatedItem, oldQuantity, mOperationResult, null);
    }

    // decreaseQuantity method
    // Takes an item as a parameter
    // Decreases the item's quantity by 1
    // Posts CANNOT_DECREASE if the item's quantity is already 0
    // Calls updateItem repository method to make the update
    // Called by minus button in InventoryGridFragment/InventoryItemAdapter
    // Repository method posts the result to OperationResult
    public void decreaseQuantity(Item item) {
        // Skip the operation if the item's quantity is already 0
        // to prevent negative quantity values
        if (item.getQuantity() > 0) { // Greater than 0
            int oldQuantity = item.getQuantity();
            Item updatedItem = new Item(item.getName(), item.getDescription(), oldQuantity - 1);
            updatedItem.setID(item.getID());
            // Passes mLowStockItemName to UpdateItem in case quantity is being decremented to 0
            mRepo.updateItem(updatedItem, oldQuantity, mOperationResult, mLowStockItemName);
        } else { // Quantity is 0, skip operation and post CANNOT_DECREASE
            mOperationResult.setValue(OperationResult.CANNOT_DECREASE);
        }
    }

    // resetLowStockItemName method
    // Resets mLowStockItemName to null
    // Called by ItemDetailsFragment after processing the notification
    // to prevent duplicate notifications
    public void resetLowStockItemName() {
        mLowStockItemName.setValue(null);
    }

    // deleteItem method
    // Takes an item as a parameter
    // Calls deleteItem method from ItemRepository to delete the item
    // Repository method posts the result to OperationResult
    public void deleteItem(Item item) {
        mRepo.deleteItem(item, mOperationResult);
    }
}
