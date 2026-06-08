package com.bencobble.inventorytracker.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bencobble.inventorytracker.model.AuditLog;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.model.QuantityHistory;
import com.bencobble.inventorytracker.util.SearchTokensBuilder;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

// ItemRepository class
// Handles all data operations for items
// Singleton instance managed by Hilt
@Singleton
public class ItemRepository {
    private static final String TAG = "ItemRepository";
    private static final int PAGE_SIZE = 50;

    private final FirebaseFirestore mFirestore;
    private final UserRepository mUserRepo;

    private ListenerRegistration mItemsListener;
    private ListenerRegistration mItemListener;
    private ListenerRegistration mQuantityHistoryListener;

    private final MutableLiveData<List<Item>> mAllItemsLiveData = new MutableLiveData<>();

    private String mCurrentListeningUid = null;

    private String mCurrentSearchToken = null;
    private DocumentSnapshot mLastVisibleDoc = null;
    private boolean mIsLoadingMore = false;
    private boolean mHasMore = true;

    /*Constructor*/
    @Inject
    public ItemRepository(UserRepository userRepo) {
        mFirestore = FirebaseFirestore.getInstance();
        mUserRepo = userRepo;

        // AuthStateListener listens for changes in the user's authentication state
        // If the user logs in, starts listening to their data and resets search and pagination
        // If the user logs out, stops listening to their data
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) { // Logged in
                mCurrentSearchToken = null;
                mLastVisibleDoc = null;
                mHasMore = true;
                startItemsListener(user.getUid());
            } else { // Logged out
                stopItemsListener();
            }
        });
    }

    /* ItemsListener methods */

    // startItemsListener method
    // Listens to changes in the user's items
    //   - Browse mode (no search token): first PAGE_SIZE items, ordered by name
    //   - Search mode (search token set): items whose searchTokens array contains the token
    // Tears down any prior listener first so only one is active at a time
    private void startItemsListener(String uid) {
        // Stop any existing listeners before starting a new one
        // Prevents more than one listener from existing
        stopItemsListener();

        mCurrentListeningUid = uid;

        // Build the items query
        // If there is a search query, filter the query by prefix token
        // otherwise, query all items up to PAGE_SIZE
        Query query = getItemsCollection(uid).orderBy("name", Query.Direction.ASCENDING);
        if (mCurrentSearchToken != null) {
            // Search query
            query = query.whereArrayContains("searchTokens", mCurrentSearchToken).limit(PAGE_SIZE);
        } else {
            // No search query
            query = query.limit(PAGE_SIZE);
        }

        // Attaches the Firestore snapshot listener
        // to the user's items collection
        mItemsListener = query.addSnapshotListener((snapshots, error) -> {
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

                // Update mLastVisibleDoc with the last loaded document if viewing items
                // without any search filtering
                // Update mHasMore bool based on whether there are more items to load
                if (mCurrentSearchToken == null) {
                    int size = snapshots.size(); // Size of items loaded in the snapshot
                    mLastVisibleDoc = size > 0 ? snapshots.getDocuments().get(size - 1) : null;
                    // If PAGE_SIZE items were fetched, set mHasMore to true to indicate
                    // that there are more items, otherwise false
                    mHasMore = size == PAGE_SIZE;
                }
            }

            mAllItemsLiveData.postValue(itemList);
        });
    }

    // stopItemsListener method
    // Stops the snapshot listener from startItemsListener and resets pagination state
    // Prevents showing another user's data
    // if they were previously logged in
    private void stopItemsListener() {
        if (mItemsListener != null) {
            mItemsListener.remove();
            mItemsListener = null;
        }

        // Reset state
        mCurrentListeningUid = null;
        mLastVisibleDoc = null;
        mHasMore = true;
        mIsLoadingMore = false;
        mAllItemsLiveData.postValue(new ArrayList<>());
    }

    /* Search and pagination methods */

    // setSearchQuery method
    // Sets the current search query
    // Re-attaches the listener to switch to query mode
    public void setSearchQuery(String query) {
        String token = SearchTokensBuilder.normalizeQuery(query);

        // Skip if the token wasn't changed
        if (Objects.equals(token, mCurrentSearchToken)) {
            return;
        }

        mCurrentSearchToken = token;

        // Re-attach the listener to start query mode
        // Only if the user is logged in
        String uid = mUserRepo.getCurrentUserId();
        if (uid != null) {
            startItemsListener(uid);
        }
    }

    // loadMore method
    // Fetches the next page of items from Firestore
    public void loadMore() {
        // Skip if in search mode or there are no more items to load
        if (mCurrentSearchToken != null) return;
        if (mIsLoadingMore || !mHasMore || mLastVisibleDoc == null) return;

        String uid = mUserRepo.getCurrentUserId();
        if (uid == null) return;

        mIsLoadingMore = true;

        // Fetch the next page of items starting at the document after the last one previously loaded
        getItemsCollection(uid)
                .orderBy("name", Query.Direction.ASCENDING)
                .startAfter(mLastVisibleDoc)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snapshots -> {
                    appendPage(snapshots);
                    mIsLoadingMore = false;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading more items: " + e.getMessage(), e);
                    mIsLoadingMore = false;
                });
    }

    // appendPage helper method
    // Appends the fetched page to the current LiveData and updates mLastVisibleDoc and mHasMore
    private void appendPage(QuerySnapshot snapshots) {
        List<Item> current = mAllItemsLiveData.getValue();
        List<Item> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            Item item = doc.toObject(Item.class);
            if (item != null) {
                updated.add(item);
            }
        }

        int size = snapshots.size();
        if (size > 0) {
            mLastVisibleDoc = snapshots.getDocuments().get(size - 1);
        }
        mHasMore = size == PAGE_SIZE;

        mAllItemsLiveData.postValue(updated);
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

        // Populate the prefix tokens for server-side search
        item.setSearchTokens(SearchTokensBuilder.tokenizeName(item.getName()));

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

        // Rebuild the prefix tokens in case the name was edited
        item.setSearchTokens(SearchTokensBuilder.tokenizeName(item.getName()));

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
