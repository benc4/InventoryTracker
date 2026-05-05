package com.bencobble.inventorytracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

// Item details screen
public class ItemDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_ITEM_ID = "item_id";

    private EditText mEditTextItemName;
    private EditText mEditTextDescription;
    private EditText mEditTextQuantity;
    private ImageButton mButtonIncrease;
    private ImageButton mButtonDecrease;
    private TextView mTextViewStatus;
    private Button mButtonEdit;
    private Space mSpace;
    private Button mButtonSave;

    private InventoryViewModel mInventoryViewModel;
    private Item mItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        mEditTextItemName = findViewById(R.id.editTextItemName);
        mEditTextDescription = findViewById(R.id.editTextDescription);
        mEditTextQuantity = findViewById(R.id.editTextQuantity);
        mButtonIncrease = findViewById(R.id.buttonIncrease);
        mButtonDecrease = findViewById(R.id.buttonDecrease);
        mButtonIncrease.setEnabled(false);
        mButtonDecrease.setEnabled(false);
        mTextViewStatus = findViewById(R.id.textViewStatus);
        mButtonEdit = findViewById(R.id.buttonEdit);
        mSpace = findViewById(R.id.space);
        Button mButtonDelete = findViewById(R.id.buttonDelete);
        mButtonSave = findViewById(R.id.buttonSave);

        mInventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Get item ID from intent
        long itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);

        // Load item from database using itemId passed from intent
        mInventoryViewModel.getItem(itemId).observe(this, item -> {
            if (item == null || itemId == -1) { // Item not found
                finish(); // End activity and return to InventoryGridActivity
                return;
            }

            // Set mItem to item retrieved from database
            mItem = item;

            // Inflate fields with item details
            displayItem();
        });

        // Observe changes to mOperationResult after updateItem or deleteItem run
        mInventoryViewModel.getOperationResult().observe(this, result -> {
            if (result != null) { // Don't run when activity is created
                processResult(result);
            }
        });

        /* Button Listeners */

        // Increase button
        mButtonIncrease.setOnClickListener(view -> {
            // Set quantity to 1 if field is empty
            if (mEditTextQuantity.getText().toString().trim().isEmpty()) {
                mEditTextQuantity.setText("1");
                return;
            }

            // Convert quantity string to int and increment it in EditText
            int quantity = Integer.parseInt(mEditTextQuantity.getText().toString().trim());
            mEditTextQuantity.setText(String.valueOf(quantity + 1));
        });

        // Decrease button
        mButtonDecrease.setOnClickListener(view -> {
            // Set quantity to 0 if field is empty
            if (mEditTextQuantity.getText().toString().trim().isEmpty()) {
                mEditTextQuantity.setText("0");
                return;
            }

            // Convert quantity string to int and decrement it in EditText
            int quantity = Integer.parseInt(mEditTextQuantity.getText().toString().trim());
            if (quantity > 0) { // Only decrement if quantity is greater than 0
                mEditTextQuantity.setText(String.valueOf(quantity - 1));
            } else {
                Toast.makeText(this, R.string.cannot_decrease, Toast.LENGTH_SHORT).show();
            }
        });

        // Edit button
        mButtonEdit.setOnClickListener(view -> {
            editMode(); // Switch to editing UI
        });

        // Save button
        mButtonSave.setOnClickListener(view -> {
            // Get all fields from EditText
            String name = mEditTextItemName.getText().toString().trim();
            String description = mEditTextDescription.getText().toString().trim();
            String quantityStr = mEditTextQuantity.getText().toString().trim();

            // Save changes to database
            mInventoryViewModel.updateItem(mItem, name, description, quantityStr);
        });

        // Delete button
        mButtonDelete.setOnClickListener(view -> {
            // Show delete confirmation dialogue
            new AlertDialog.Builder(this)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete " + mItem.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        mInventoryViewModel.deleteItem(mItem); // Delete item from database
                    })
                    .setNegativeButton("Cancel", null) // Do nothing on cancel
                    .create()
                    .show();
        });
    }

    // Populates EditText fields with mItem values
    private void displayItem() {
        mEditTextItemName.setText(mItem.getName());
        mEditTextDescription.setText(mItem.getDescription());
        mEditTextQuantity.setText(String.valueOf(mItem.getQuantity()));
    }

    // Sets up editing UI
    private void editMode() {
        // Enable all fields for editing
        mEditTextItemName.setEnabled(true);
        mEditTextDescription.setEnabled(true);
        mEditTextQuantity.setEnabled(true);
        mButtonIncrease.setEnabled(true);
        mButtonDecrease.setEnabled(true);

        // Hide edit button and show save button
        mButtonSave.setVisibility(Button.VISIBLE);
        mButtonEdit.setVisibility(Button.GONE);
        mSpace.setVisibility(Space.GONE); // Remove space that separated edit/delete buttons
    }

    // Returns to viewing UI
    private void viewMode() {
        // Hide error message
        mTextViewStatus.setText("");
        mTextViewStatus.setVisibility(TextView.GONE);

        // Disable editing
        mEditTextItemName.setEnabled(false);
        mEditTextDescription.setEnabled(false);
        mEditTextQuantity.setEnabled(false);
        mButtonIncrease.setEnabled(false);
        mButtonDecrease.setEnabled(false);

        // Hide save button and show edit button
        mButtonSave.setVisibility(Button.GONE);
        mButtonEdit.setVisibility(Button.VISIBLE);
        mSpace.setVisibility(Space.VISIBLE);
    }

    // Handles result code from mOperationResult
    private void processResult(InventoryViewModel.OperationResult result) {
        switch (result) {
            case SUCCESS: // Update successful
                setResult(RESULT_CANCELED); // Removes low stock result from intent if there was one
                viewMode(); // Switch back from edit UI to viewing
                break;
            case SUCCESS_LOW_STOCK: // Item has been updated with quantity of 0
                // Add low stock item to InventoryGridActivity result intent
                Intent resultIntent = new Intent();
                resultIntent.putExtra("item_low_stock", mItem.getName());
                setResult(RESULT_OK, resultIntent);
                viewMode(); // Switch back from edit UI to viewing
                break;
            case SUCCESS_DELETE:
                setResult(RESULT_CANCELED); // Removes low stock result from intent if there was one
                finish(); // End activity and return to InventoryGridActivity
                break;
            case UPDATE_FAILED:
                mTextViewStatus.setText(R.string.update_item_failed);
                mTextViewStatus.setVisibility(TextView.VISIBLE);
                break;
            case EMPTY_FIELDS:
                mTextViewStatus.setText(R.string.update_item_empty);
                mTextViewStatus.setVisibility(TextView.VISIBLE);
                break;
        }
    }
}