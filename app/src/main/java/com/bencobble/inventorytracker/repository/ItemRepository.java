package com.bencobble.inventorytracker.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bencobble.inventorytracker.model.AuditLog;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.model.QuantityHistory;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

// ItemRepository class
// Handles all data operations for items
// Singleton instance managed by Hilt
@Singleton
public class ItemRepository {
    private static final String TAG = "ItemRepository";

    private final FirebaseFirestore mFirestore;
    private final UserRepository mUserRepo;

    private ListenerRegistration mItemsListener;
    private ListenerRegistration mItemListener;
    private ListenerRegistration mQuantityHistoryListener;

    private final MutableLiveData<List<Item>> mAllItemsLiveData = new MutableLiveData<>();

    private String mCurrentListeningUid = null;

    /*Constructor*/
    @Inject
    public ItemRepository(UserRepository userRepo) {
        mFirestore = FirebaseFirestore.getInstance();
        mUserRepo = userRepo;

        // AuthStateListener listens for changes in the user's authentication state
        // If the user logs in, starts listening to their data
        // If the user logs out, stops listening to their data
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) { // Logged in
                startItemsListener(user.getUid());
            } else { // Logged out
                stopItemsListener();
            }
        });
    }

    /* ItemsListener methods */

    // startItemsListener method
    // Listens to changes in the user's items
    // by attaching a Firestore snapshot listener
    // Listener is active the whole time the user is logged in
    // Posts current items list to mAllItemsLiveData
    private void startItemsListener(String uid) {
        // Skip if already listening to this user's data
        if (mItemsListener != null && uid.equals(mCurrentListeningUid)) {
            return;
        }

        // Stop any existing listeners before starting a new one
        // Prevents more than one listener from existing
        stopItemsListener();

        // Assign listening id to current user
        mCurrentListeningUid = uid;

        // Attaches the Firestore snapshot listener
        // to the user's items collection
        mItemsListener = mFirestore.collection("users").document(uid).collection("items")
                .orderBy("name", Query.Direction.ASCENDING)

                .addSnapshotListener((snapshots, error) -> {
                    // Log error and return on failure
                    // Prevents bad data in the list
                    if (error != null) {
                        Log.e(TAG, "Error fetching items: " + error.getMessage(), error);
                        return;
                    }

                    // Convert the snapshot items to a list of Item objects
                    List<Item> itemList = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Item item = doc.toObject(Item.class);
                            if (item != null) {
                                itemList.add(item);
                            }
                        }
                    }

                    // Post the updated list to the LiveData
                    mAllItemsLiveData.postValue(itemList);
                });
    }

    // stopItemsListener method
    // Stops the snapshot listener from startItemsListener
    // Prevents showing another user's data
    // if they were previously logged in
    private void stopItemsListener() {
        // If the listener exists,
        // Remove it and set it to null
        if (mItemsListener != null) {
            mItemsListener.remove();
            mItemsListener = null;
        }

        // Reset listening user id and clear item list
        mCurrentListeningUid = null;
        mAllItemsLiveData.postValue(new ArrayList<>());
    }

    /* Firebase CollectionReference helpers */

    private CollectionReference getItemsCollection(String uid) {
        return mFirestore.collection("users").document(uid).collection("items");
    }

    private CollectionReference getAuditLogCollection(String uid) {
        return mFirestore.collection("users").document(uid).collection("auditLog");
    }

    private CollectionReference getHistoryCollection(String uid, String itemId) {
        return getItemsCollection(uid).document(itemId).collection("history");
    }

    /* AuditLog methods */

    // writeAuditLog method
    // Writes an audit log entry to the user's AuditLog collection
    // Called when an item is added, updated, or deleted
    // Includes the uid of who made the change, item's id, action, and details
    private void writeAuditLog(String uid, String itemId, String action, String details) {
        // Builds the log object, finds the user's AuditLog collection, adds the object
        AuditLog log = new AuditLog(uid, itemId, action, details);
        getAuditLogCollection(uid).add(log)
                .addOnFailureListener(e -> Log.e(TAG, "Error writing audit log: " + e.getMessage(), e));
    }

    /* QuantityHistory methods */

    // writeQuantityHistory method
    // Writes a quantity history entry to the item's QuantityHistory collection
    // Called when an item's quantity is updated or initially set
    // Includes the uid of who made the change, item's id, old quantity, and new quantity
    private void writeQuantityHistory(String uid, String itemId, int oldQty, int newQty) {
        if (oldQty == newQty) return; // Skip if no change was made

        // Builds the history object, finds the item's QuantityHistory collection, adds the object
        QuantityHistory history = new QuantityHistory(oldQty, newQty, uid);
        getHistoryCollection(uid, itemId).add(history)
                .addOnFailureListener(e -> Log.e(TAG, "Error writing quantity history: " + e.getMessage(), e));
    }

    /* Item methods */

    // getItems method
    // Returns current mAllItemsLiveData set by the snapshot listener
    public LiveData<List<Item>> getItems() {
        return mAllItemsLiveData;
    }

    // getItem method
    // Takes item ID as parameter
    // Sets up a snapshot listener to listen for changes to the item
    // Returns matching item from the user's items collection as LiveData
    public LiveData<Item> getItem(String id) {
        MutableLiveData<Item> itemData = new MutableLiveData<>();

        // Fetch user id and skip item fetching if null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null || id == null) {
            return itemData;
        }

        // Stop any existing listeners before starting a new one
        // Prevents more than one listener from existing
        stopItemListener();

        getItemsCollection(uid).document(id)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        itemData.postValue(null);
                        return;
                    }

                    Item item = snapshot.toObject(Item.class);
                    itemData.postValue(item);
                });

        return itemData;
    }

    // stopItemListener helper method
    // Stops the snapshot listener from getItem
    // Called when ItemDetailsFragment is destroyed
    public void stopItemListener() {
        if (mItemListener != null) {
            mItemListener.remove();
            mItemListener = null;
        }
    }

    // addItem method
    // Takes an Item and OperationResult LiveData as parameters
    // Attempts to add the item to the user's items collection
    // Posts result as a status from OperationResult to the LiveData
    // Writes an AuditLog and QuantityHistory log on success
    public void addItem(Item item, MutableLiveData<InventoryViewModel.OperationResult> result) {
        // Fetch user id and skip deletion if null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null) {
            result.postValue(InventoryViewModel.OperationResult.ADD_FAILED);
            return;
        }

        // Add item to user's items collection
        getItemsCollection(uid).add(item)
                .addOnSuccessListener(docRef -> {
                    item.setID(docRef.getId()); // Sets the ID of the new item

                    // Logs the change in AuditLog
                    writeAuditLog(uid, docRef.getId(), "INSERT",
                            "Added " + item.getName() + " (qty: " + item.getQuantity() + ")");

                    // Logs the change in QuantityHistory
                    writeQuantityHistory(uid, docRef.getId(), 0, item.getQuantity());

                    // Posts success to OperationResult
                    result.postValue(InventoryViewModel.OperationResult.SUCCESS);
                })
                // Database operation error
                .addOnFailureListener(e ->
                        result.postValue(InventoryViewModel.OperationResult.ADD_FAILED));
    }

    // updateItem method
    // Takes an Item, its pre-update quantity, OperationResult LiveData, and lowStockItemName LiveData as parameters
    // Attempts to update the item in the user's items collection
    // Posts result as a status from OperationResult to the LiveData
    // Writes an AuditLog and QuantityHistory log on success
    public void updateItem(Item item, int oldQuantity,
                           MutableLiveData<InventoryViewModel.OperationResult> result,
                           MutableLiveData<String> lowStockItemName) {

        // Fetch user id and skip update if null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null) {
            result.postValue(InventoryViewModel.OperationResult.UPDATE_FAILED);
            return;
        }

        // Update item in user's items collection
        getItemsCollection(uid).document(item.getID()).set(item)
                .addOnSuccessListener(aVoid -> {

                    // Logs the change in AuditLog
                    writeAuditLog(uid, item.getID(), "UPDATE",
                            "Updated " + item.getName() + " (qty: " + oldQuantity + " -> " + item.getQuantity() + ")");

                    // Logs the change in QuantityHistory
                    writeQuantityHistory(uid, item.getID(), oldQuantity, item.getQuantity());

                    // If the item's quantity is set to 0, add the item's name to lowStockItemName
                    if (item.getQuantity() == 0 && lowStockItemName != null) {
                        lowStockItemName.postValue(item.getName());
                    }

                    // Posts success result to OperationResult
                    // Includes low stock indicator if the item's quantity is 0
                    if (item.getQuantity() == 0) {
                        result.postValue(InventoryViewModel.OperationResult.SUCCESS_LOW_STOCK);
                    } else {
                        result.postValue(InventoryViewModel.OperationResult.SUCCESS);
                    }
                })
                // Database operation error
                .addOnFailureListener(e ->
                        result.postValue(InventoryViewModel.OperationResult.UPDATE_FAILED));
    }

    // deleteItem method
    // Takes an Item and OperationResult LiveData as parameters
    // Attempts to delete the item from the user's items collection
    // Posts result as a status from OperationResult to the LiveData
    // Writes an AuditLog on success
    public void deleteItem(Item item, MutableLiveData<InventoryViewModel.OperationResult> result) {
        // Fetch user id and skip deletion if null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null) {
            result.postValue(InventoryViewModel.OperationResult.DELETE_FAILED);
            return;
        }

        // Delete item from user's items collection
        getItemsCollection(uid).document(item.getID()).delete()
                .addOnSuccessListener(aVoid -> {
                    // Logs the change in AuditLog
                    writeAuditLog(uid, item.getID(), "DELETE", "Deleted " + item.getName());
                    result.postValue(InventoryViewModel.OperationResult.SUCCESS_DELETE);
                })
                // Database operation error
                .addOnFailureListener(e ->
                        result.postValue(InventoryViewModel.OperationResult.DELETE_FAILED));
    }

    // getQuantityHistory method
    // Takes an Item and OperationResult LiveData as parameters
    // Sets up a snapshot listener to listen for changes to the item's QuantityHistory collection
    // Returns matching list of QuantityHistory objects as LiveData
    // Posts result as a status from OperationResult to the LiveData
    public LiveData<List<QuantityHistory>> getQuantityHistory(String id, MutableLiveData<InventoryViewModel.OperationResult> result) {
        MutableLiveData<List<QuantityHistory>> historyData = new MutableLiveData<>();

        // Fetch user id and skip fetching if uid or item id null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null || id == null) {
            result.postValue(InventoryViewModel.OperationResult.GET_QUANTITY_HISTORY_FAILED);
            return historyData;
        }

        // Fetch QuantityHistory objects from user's items' history collection
        getHistoryCollection(uid, id)
                // Sort by timestamp in ascending order
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    // Post failure code, log error, and return on failure
                    if (error != null) {
                        result.postValue(InventoryViewModel.OperationResult.GET_QUANTITY_HISTORY_FAILED);
                        Log.e(TAG, "Error fetching quantity history", error);
                        return;
                    }

                    // Convert the snapshot items to a list of QuantityHistory objects
                    List<QuantityHistory> historyList = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            QuantityHistory entry = doc.toObject(QuantityHistory.class);
                            if (entry != null) {
                                historyList.add(entry);
                            }
                        }
                    }
                    // Post the updated list to the LiveData
                    historyData.postValue(historyList);
                });

        return historyData;
    }

    // stopQuantityHistoryListener helper method
    // Stops the snapshot listener from getQuantityHistory
    // Called when ItemDetailsFragment is destroyed
    public void stopQuantityHistoryListener() {
        if (mQuantityHistoryListener != null) {
            mQuantityHistoryListener.remove();
            mQuantityHistoryListener = null;
        }
    }
}
