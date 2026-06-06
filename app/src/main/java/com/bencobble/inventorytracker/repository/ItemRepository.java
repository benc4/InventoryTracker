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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

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

    // addAuditLogToBatch method
    // Adds an audit log write to a Firestore WriteBatch
    // Includes the uid of who made the change, item's id, action, and details
    // Called when an item is added, updated, or deleted
    private void addAuditLogToBatch(WriteBatch batch, String uid, String itemId,
                                    String action, String details) {
        // Fetch a document reference for the auditLog on the client side
        DocumentReference logRef = getAuditLogCollection(uid).document();

        // Build the log object and add the write to the batch
        AuditLog log = new AuditLog(uid, itemId, action, details);
        batch.set(logRef, log);
    }

    /* QuantityHistory methods */

    // addQuantityHistoryToBatch method
    // Adds a quantity history write to a Firestore WriteBatch
    // Called when an item's quantity is updated or initially set
    // Includes the uid of who made the change, item's id, old quantity, and new quantity
    private void addQuantityHistoryToBatch(WriteBatch batch, String uid, String itemId,
                                           int oldQty, int newQty) {
        if (oldQty == newQty) return; // Skip if no change was made

        // Fetch a document reference for the history document on the client side
        DocumentReference historyRef = getHistoryCollection(uid, itemId).document();

        // Build the history object and add the write to the batch
        QuantityHistory history = new QuantityHistory(oldQty, newQty, uid);
        batch.set(historyRef, history);
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
    // Writes the new item, an audit log entry, and a quantity history
    // entry in a Firestore WriteBatch so all three writes are synced
    // Posts result as a status from OperationResult to the LiveData
    public void addItem(Item item, MutableLiveData<InventoryViewModel.OperationResult> result) {
        // Fetch user id and skip the add if null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null) {
            result.postValue(InventoryViewModel.OperationResult.ADD_FAILED);
            return;
        }

        // Generate the new item's document reference before the WriteBatch
        DocumentReference itemRef = getItemsCollection(uid).document();
        item.setID(itemRef.getId());

        // Build the batch with all 3 writes
        WriteBatch batch = mFirestore.batch();
        batch.set(itemRef, item);
        addAuditLogToBatch(batch, uid, itemRef.getId(), "INSERT",
                "Added " + item.getName() + " (qty: " + item.getQuantity() + ")");
        // oldQty is 0 for the initial/pre-addition quantity
        addQuantityHistoryToBatch(batch, uid, itemRef.getId(), 0, item.getQuantity());

        // Commit the batch in one write
        batch.commit()
                .addOnSuccessListener(aVoid ->
                        result.postValue(InventoryViewModel.OperationResult.SUCCESS))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error committing addItem batch: " + e.getMessage(), e);
                    result.postValue(InventoryViewModel.OperationResult.ADD_FAILED);
                });
    }

    // updateItem method
    // Takes an Item, its pre-update quantity, OperationResult LiveData, and lowStockItemName LiveData as parameters
    // Writes the updated item, an audit log entry and a quantity history
    // entry in a Firestore WriteBatch so all three writes are synced
    // Posts result as a status from OperationResult to the LiveData
    public void updateItem(Item item, int oldQuantity,
                           MutableLiveData<InventoryViewModel.OperationResult> result,
                           MutableLiveData<String> lowStockItemName) {

        // Fetch user id and skip update if null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null) {
            result.postValue(InventoryViewModel.OperationResult.UPDATE_FAILED);
            return;
        }

        // Build the batch with all 3 writes
        DocumentReference itemRef = getItemsCollection(uid).document(item.getID());
        WriteBatch batch = mFirestore.batch();
        batch.set(itemRef, item);
        addAuditLogToBatch(batch, uid, item.getID(), "UPDATE",
                "Updated " + item.getName() + " (qty: " + oldQuantity + " -> " + item.getQuantity() + ")");
        addQuantityHistoryToBatch(batch, uid, item.getID(), oldQuantity, item.getQuantity());

        // Commit the batch in one write
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // If the item's quantity is set to 0, add the item's name to lowStockItemName
                    if (item.getQuantity() == 0 && lowStockItemName != null) {
                        lowStockItemName.postValue(item.getName());
                    }

                    // Posts success result to OperationResult
                    if (item.getQuantity() == 0) {
                        result.postValue(InventoryViewModel.OperationResult.SUCCESS_LOW_STOCK);
                    } else {
                        result.postValue(InventoryViewModel.OperationResult.SUCCESS);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error committing updateItem batch: " + e.getMessage(), e);
                    result.postValue(InventoryViewModel.OperationResult.UPDATE_FAILED);
                });
    }

    // deleteItem method
    // Takes an Item and OperationResult LiveData as parameters
    // Deletes the item from the user's items collection and writes an
    // audit log entry in a WriteBatch
    // Posts result as a status from OperationResult to the LiveData
    public void deleteItem(Item item, MutableLiveData<InventoryViewModel.OperationResult> result) {
        // Fetch user id and skip deletion if null
        String uid = mUserRepo.getCurrentUserId();
        if (uid == null) {
            result.postValue(InventoryViewModel.OperationResult.DELETE_FAILED);
            return;
        }

        // Build the batch with deletion and audit log entry
        DocumentReference itemRef = getItemsCollection(uid).document(item.getID());
        WriteBatch batch = mFirestore.batch();
        batch.delete(itemRef);
        addAuditLogToBatch(batch, uid, item.getID(), "DELETE", "Deleted " + item.getName());

        // Commit the batch in one write
        batch.commit()
                .addOnSuccessListener(aVoid ->
                        result.postValue(InventoryViewModel.OperationResult.SUCCESS_DELETE))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error committing deleteItem batch: " + e.getMessage(), e);
                    result.postValue(InventoryViewModel.OperationResult.DELETE_FAILED);
                });
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
