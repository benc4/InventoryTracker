package com.bencobble.inventorytracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

import dagger.hilt.android.AndroidEntryPoint;

// AddItemFragment
// Handles user input and UI for the add item screen
// Dependency injection managed by Hilt
@AndroidEntryPoint
public class AddItemFragment extends Fragment {
    /* View references */
    private EditText mEditTextItemName;
    private EditText mEditTextDescription;
    private EditText mEditTextQuantity;
    private TextView mTextViewError;

    private InventoryViewModel mInventoryViewModel;

    // onCreateView override method
    // Inflates the layout XML
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_add_item, container, false);
    }

    // onViewCreated override method
    // Handles setup after the view is created
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEditTextItemName = view.findViewById(R.id.editTextItemName);
        mEditTextDescription = view.findViewById(R.id.editTextDescription);
        mEditTextQuantity = view.findViewById(R.id.editTextQuantity);
        mTextViewError = view.findViewById(R.id.textViewError);
        Button buttonCreate = view.findViewById(R.id.buttonCreateItem);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);

        // Gets InventoryViewModel
        // ItemRepository is injected into it by Hilt
        // The ViewModel is scoped to only this fragment and
        // item LiveData is shared between fragments through ItemRepository
        mInventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // getOperationResult observer
        // Observes changes to the OperationResult LiveData
        // Calls processResult to handle the OperationResult when it updates
        mInventoryViewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {  // Skip if result is null (fragment first created)
                processResult(result);
            }
        });

        // Listener for the Create button
        // Retrieves name, description, and quantity from EditText fields
        // Calls addItem method in InventoryViewModel with them
        buttonCreate.setOnClickListener(v -> {
            // Clears error message to prevent old messages on retry
            mTextViewError.setText("");
            mTextViewError.setVisibility(TextView.GONE);

            // Retrieve item values
            String name = mEditTextItemName.getText().toString().trim();
            String description = mEditTextDescription.getText().toString().trim();
            String quantityStr = mEditTextQuantity.getText().toString().trim();

            // Call addItem method in InventoryViewModel
            mInventoryViewModel.addItem(name, description, quantityStr);
        });

        // Listener for the Cancel button
        // Removes the fragment from the navigation back stack
        // to return to InventoryGridActivity
        buttonCancel.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
    }

    // processResult method
    // Takes an OperationResult code as a parameter
    // Called by the getOperationResult observer
    // to handle the OperationResult code when it updates
    // Navigates back to InventoryGridActivity on success,
    // shows error message otherwise
    private void processResult(InventoryViewModel.OperationResult result) {
        switch (result) {
            case SUCCESS:
                Navigation.findNavController(requireView()).popBackStack();
                return;
            case ADD_FAILED:
                mTextViewError.setText(R.string.add_item_failed);
                break;
            case EMPTY_FIELDS:
                mTextViewError.setText(R.string.update_item_empty);
                break;
            case INVALID_QUANTITY:
                mTextViewError.setText(R.string.invalid_quantity);
                break;
            default:
                mTextViewError.setText(R.string.something_went_wrong);
                break;
        }
        mTextViewError.setVisibility(TextView.VISIBLE); // Show error message in UI
    }
}
