package com.bencobble.inventorytracker.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

// Add item screen
public class AddItemActivity extends AppCompatActivity {
    private EditText mEditTextItemName;
    private EditText mEditTextDescription;
    private EditText mEditTextQuantity;
    private TextView mTextViewError;
    private InventoryViewModel mInventoryViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        mEditTextItemName = findViewById(R.id.editTextItemName);
        mEditTextDescription = findViewById(R.id.editTextDescription);
        mEditTextQuantity = findViewById(R.id.editTextQuantity);
        mTextViewError = findViewById(R.id.textViewError);
        Button mButtonCreate = findViewById(R.id.buttonCreateItem);
        Button mButtonCancel = findViewById(R.id.buttonCancel);

        mInventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Observe changes to mOperationResult after addItem runs
        mInventoryViewModel.getOperationResult().observe(this, result -> {
            if (result != null) { // Don't run when activity is created
                processResult(result);
            }
        });

        /* Button Listeners */

        // Create button
        mButtonCreate.setOnClickListener(view -> {
            // Clear error message
            mTextViewError.setText("");
            mTextViewError.setVisibility(TextView.GONE);

            // Get values from ExitText fields
            String name = mEditTextItemName.getText().toString().trim();
            String description = mEditTextDescription.getText().toString().trim();
            String quantityStr = mEditTextQuantity.getText().toString().trim();

            // Add item to database
            mInventoryViewModel.addItem(name, description, quantityStr);
        });

        // Cancel button
        // Ends activity and returns to InventoryGridActivity
        mButtonCancel.setOnClickListener(view -> finish());
    }

    // Handles result code from mOperationResult
    private void processResult(InventoryViewModel.OperationResult result) {
        switch (result) {
            case SUCCESS:
                finish(); // End activity and return to InventoryGridActivity
                return;
            case ADD_FAILED:
                mTextViewError.setText(R.string.add_item_failed);
                break;
            case EMPTY_FIELDS:
                mTextViewError.setText(R.string.update_item_empty);
                break;
        }
        // Show error message
        mTextViewError.setVisibility(TextView.VISIBLE);
    }
}