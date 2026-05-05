package com.bencobble.inventorytracker.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import com.bencobble.inventorytracker.database.InventoryTrackerDatabase;
import com.bencobble.inventorytracker.database.ItemDao;
import com.bencobble.inventorytracker.database.UserDao;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.model.User;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;
import com.bencobble.inventorytracker.viewmodel.LoginViewModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Repository class
// Makes calls to the database
public class InventoryRepository {
    private static InventoryRepository mInventoryRepo;
    private final UserDao mUserDao;
    private final ItemDao mItemDao;

    // Executor for database operations on background threads
    private static final ExecutorService mExecutor = Executors.newFixedThreadPool(4);

    // Singleton instance
    public static InventoryRepository getInstance(Context context) {
        if (mInventoryRepo == null) {
            mInventoryRepo = new InventoryRepository(context);
        }
        return mInventoryRepo;
    }

    private InventoryRepository(Context context) {
        InventoryTrackerDatabase database = Room.databaseBuilder(
                context, InventoryTrackerDatabase.class, "inventory_tracker.db")
                .build();

        mUserDao = database.userDao();
        mItemDao = database.itemDao();
    }

    /*
    User methods
    */

    // Login method
    // Takes username and password along with UserResult LiveData as parameters
    // Posts a status from UserResult to the result LiveData
    public void login(String username, String password, MutableLiveData<LoginViewModel.UserResult> result) {
        mExecutor.execute(() -> { // Runs on a background thread
            try {
                // Get User object from db associated with provided username
                User user = mUserDao.getUser(username);

                if (user == null) { // Username not in db
                    result.postValue(LoginViewModel.UserResult.USER_NOT_FOUND);
                } else if (user.getPassword().equals(password)) { // User's password matches provided password
                    result.postValue(LoginViewModel.UserResult.SUCCESS_LOGIN);
                } else { // User's password doesn't match provided password
                    result.postValue(LoginViewModel.UserResult.INVALID_CREDENTIALS);
                }
            } catch (Exception e) { // Database operation error
                result.postValue(LoginViewModel.UserResult.DATABASE_ERROR);
            }
        });
    }

    // createAccount method
    // Takes username and password along with UserResult LiveData as parameters
    // Posts a status from UserResult to the result LiveData
    public void createAccount(String username, String password, MutableLiveData<LoginViewModel.UserResult> result) {
        mExecutor.execute(() -> { // Runs on a background thread
            try {
                // Get User object from db associated with provided username
                // If it exists, username is taken
                User existingUser = mUserDao.getUser(username);
                if (existingUser != null) {
                    result.postValue(LoginViewModel.UserResult.USERNAME_TAKEN);
                    return;
                }

                // Create User object with provided details and add to db
                User newUser = new User(username, password);
                mUserDao.addUser(newUser);
                result.postValue(LoginViewModel.UserResult.SUCCESS_CREATE_ACC);
            } catch (Exception e) { // Database operation error
                result.postValue(LoginViewModel.UserResult.DATABASE_ERROR);
            }
        });
    }

    /*
    Item methods
    */

    // getItems method
    // Takes OperationResult LiveData as parameter
    // Returns all items in the db as LiveData
    // Posts a status from OperationResult on exception
    // Runs on a background thread because it returns LiveData
    public LiveData<List<Item>> getItems(MutableLiveData<InventoryViewModel.OperationResult> result) {
        try {
            return mItemDao.getItems();
        } catch (Exception e) { // Database operation error
            result.postValue(InventoryViewModel.OperationResult.GET_ITEMS_FAILED);
            return null;
        }
    }

    // getItem method
    // Takes item ID as parameter
    // Returns matching item from the db as LiveData
    // Runs on a background thread because it returns LiveData
    public LiveData<Item> getItem(long id) {
        try {
            return mItemDao.getItem(id);
        } catch (Exception e) { // Database operation error
            return null;
        }
    }

    // addItem method
    // Takes an Item and OperationResult LiveData as parameters
    // Posts a status from OperationResult to the LiveData
    public void addItem(Item item, MutableLiveData<InventoryViewModel.OperationResult> result) {
        mExecutor.execute(() -> { // Runs on a background thread
            try {
                long itemId = mItemDao.addItem(item);
                item.setID(itemId); // Set the ID of the new item
                result.postValue(InventoryViewModel.OperationResult.SUCCESS);
            } catch (Exception e) { // Database operation error
                result.postValue(InventoryViewModel.OperationResult.ADD_FAILED);
            }
        });
    }

    // updateItem method
    // Takes an Item and OperationResult LiveData as parameters
    // Posts a status from OperationResult to the result LiveData
    public void updateItem(Item item, MutableLiveData<InventoryViewModel.OperationResult> result) {
        mExecutor.execute(() -> { // Runs on a background thread
            try{
                // Update item in database
                mItemDao.updateItem(item);
                if (item.getQuantity() == 0) { // Item is now low stock
                    result.postValue(InventoryViewModel.OperationResult.SUCCESS_LOW_STOCK);
                } else {
                    result.postValue(InventoryViewModel.OperationResult.SUCCESS);
                }
            } catch (Exception e) { // Database operation error
                result.postValue(InventoryViewModel.OperationResult.UPDATE_FAILED);
            }
        });
    }

    // decreaseQuantity method
    // Takes an Item, OperationResult LiveData, and LowStockItemName LiveData as parameters
    // Posts a status from OperationResult to the result LiveData
    // Posts the item name to LowStockItemName LiveData if the item's quantity is now 0
    public void decreaseQuantity(
            Item item,
            MutableLiveData<InventoryViewModel.OperationResult> result,
            MutableLiveData<String> lowStockItemName) {
        mExecutor.execute(() -> { // Runs on a background thread
            try{
                mItemDao.updateItem(item);
                if (item.getQuantity() == 0) {
                    lowStockItemName.postValue(item.getName());
                }
                result.postValue(InventoryViewModel.OperationResult.SUCCESS);
            } catch (Exception e) { // Database operation error
                result.postValue(InventoryViewModel.OperationResult.UPDATE_FAILED);
            }
        });
    }

    // deleteItem method
    // Takes an Item and OperationResult LiveData as parameters
    // Posts a status from OperationResult to the result LiveData
    public void deleteItem(Item item, MutableLiveData<InventoryViewModel.OperationResult> result) {
        mExecutor.execute(() -> { // Runs on a background thread
            try {
                mItemDao.deleteItem(item); // Delete item from database
                result.postValue(InventoryViewModel.OperationResult.SUCCESS_DELETE);
            } catch (Exception e) { // Database operation error
                result.postValue(InventoryViewModel.OperationResult.DELETE_FAILED);
            }
        });
    }
}
