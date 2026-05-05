package com.bencobble.inventorytracker.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.adapter.InventoryItemAdapter;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

// Inventory grid screen
public class InventoryGridActivity extends AppCompatActivity
        implements InventoryItemAdapter.OnItemListener {
    private static final int REQUEST_SMS_CODE = 1;
    private InventoryViewModel mInventoryViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inventory_grid);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mInventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Set up RecyclerView with adapter
        RecyclerView mRecyclerView = findViewById(R.id.recyclerViewInventory);
        RecyclerView.LayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(gridLayoutManager);

        InventoryItemAdapter adapter = new InventoryItemAdapter(mInventoryViewModel, this);
        mRecyclerView.setAdapter(adapter);

        // Observe item LiveData to update RecyclerView when items change
        mInventoryViewModel.getItems().observe(this, adapter::setItems);

        // Observe changes to mOperationResult after updateItem or deleteItem runs
        mInventoryViewModel.getOperationResult().observe(this, this::processResult);

        // Observe changes to mLowStockItemName to send low stock notification
        mInventoryViewModel.getLowStockItemName().observe(this, itemName -> {
            if (itemName != null) { // Don't run when activity is created
                sendLowStockNotification(itemName);
            }
        });

        // Set up FAB to open AddItemActivity
        findViewById(R.id.fabAddItem).setOnClickListener(view -> {
            Intent intent = new Intent(this, AddItemActivity.class);
            startActivity(intent);
        });

        // Get SMS permissions
        hasSmsPermission();
    }

    // Inflates app bar menu and sets up search bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu
        getMenuInflater().inflate(R.menu.inventory_menu, menu);

        // Inflate and set up search bar
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search items");

        // Listen for text changes in the search bar
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // Use default behavior
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Update query every time text changes
                mInventoryViewModel.setSearchQuery(newText);
                return false;
            }
        });

        // Listen for search bar expand/collapse
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            // Runs when search icon is clicked
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                return true; // Expand the search bar
            }

            // Runs when back button or collapse icon is clicked
            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                mInventoryViewModel.setSearchQuery(null); // Remove filtering
                return true; // Collapse the search bar
            }
        });
        return true;
    }

    // Listener for sign out button in app bar menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            // Show sign out confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage(R.string.sign_out_confirm)
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        // Returns to LoginActivity
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null) // Do nothing on cancel
                    .create()
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Handles the result code from mOperationResult
    private void processResult(InventoryViewModel.OperationResult result) {
        switch (result) {
            case UPDATE_FAILED:
                Toast.makeText(this, R.string.update_item_failed, Toast.LENGTH_SHORT).show();
                break;
            case CANNOT_DECREASE:
                Toast.makeText(this, R.string.cannot_decrease, Toast.LENGTH_SHORT).show();
                break;
            case DELETE_FAILED:
                Toast.makeText(this, R.string.delete_item_failed, Toast.LENGTH_SHORT).show();
                break;
            case GET_ITEMS_FAILED:
                Toast.makeText(this, R.string.get_items_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // Intent to launch ItemDetailsActivity when a card is clicked
    // Passes item ID as intent extra
    // Takes a potential low stock item from ItemDetailsActivity as result data
    @Override
    public void onItemClick(Item item) {
        Intent intent = new Intent(this, ItemDetailsActivity.class);
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_ID, item.getID());
        startActivityForResult(intent, 1);
    }

    // Sends the low stock notification when returning from ItemDetailsActivity
    // if a low stock item was put in the intent result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) { // Low stock item was passed
            String itemName = data.getStringExtra("item_low_stock");
            sendLowStockNotification(itemName); // Send notification for the item
        }
    }

    /*SMS Notification Methods*/

    // Runs after permissions are accepted or denied
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // SMS permission granted
                Toast.makeText(this, R.string.sms_accepted, Toast.LENGTH_SHORT).show();
            } else { // SMS permission denied
                Toast.makeText(this, R.string.sms_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Checks if SMS permission is granted and requests it if not
    // Returns true if granted, false if not
    private boolean hasSmsPermission() {
        String smsPermission = Manifest.permission.SEND_SMS;
        if (ContextCompat.checkSelfPermission(this, smsPermission)
                != PackageManager.PERMISSION_GRANTED) { // SMS permission not yet granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, smsPermission)) {
                // Explain why SMS permission is needed and ask again
                showPermissionRationaleDialog();
            } else { // Request permissions
                ActivityCompat.requestPermissions(this,
                        new String[] { smsPermission }, REQUEST_SMS_CODE);
            }
            return false;
        }
        return true;
    }

    // Dialogue to explain why permissions are needed
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_needed)
                .setMessage(R.string.sms_perms_rationale)
                .setPositiveButton("OK", (dialog, which) ->
                        // Request permissions again
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_CODE))
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .create()
                .show();
    }

    // Sends a low stock SMS notification for an item
    private void sendLowStockNotification(String itemName) {
        if (hasSmsPermission()) { // Only send if SMS permission is granted
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(
                        "1234",
                        null,
                        itemName + " is out of stock",
                        null,
                        null
                );
                Toast.makeText(this, getString(R.string.low_stock_noti_sent_for) + itemName, Toast.LENGTH_SHORT).show();
                Log.d("SMS", getString(R.string.low_stock_noti_sent_for) + itemName);

                // Clear mLowStockItemName after notification is sent
                mInventoryViewModel.resetLowStockItemName();
            } catch (Exception e) { // Error sending SMS
                Log.e("SMS", "Failed to send SMS: " + e.getMessage());
            }
        }
    }
}