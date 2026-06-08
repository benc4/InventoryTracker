package com.bencobble.inventorytracker.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.model.QuantityHistory;
import com.bencobble.inventorytracker.repository.ItemRepository;
import com.bencobble.inventorytracker.util.ParseIntHelper;

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
    }

    /* Item methods */

    // getItems method
    // Returns the items LiveData from the repository
    public LiveData<List<Item>> getItems() {
        return mRepo.getItems();
    }

    // setSearchQuery method
    // Take a search query string as a parameter and calls the ItemRepository setSearchQuery method
    public void setSearchQuery(String query) {
        mRepo.setSearchQuery(query);
    }

    // loadMore method
    // Call the ItemRepository loadMore method to load the next page of items
    public void loadMore() {
        mRepo.loadMore();
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
