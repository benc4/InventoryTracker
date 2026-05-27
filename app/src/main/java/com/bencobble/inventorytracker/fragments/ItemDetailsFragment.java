package com.bencobble.inventorytracker.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.model.QuantityHistory;
import com.bencobble.inventorytracker.util.NotificationHelper;
import com.bencobble.inventorytracker.util.ParseIntHelper;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

// ItemDetailsFragment
// Handles user input and UI for viewing, editing, and deleting specific items
// and displaying their quantity history chart
// Dependency injection managed by Hilt
@AndroidEntryPoint
public class ItemDetailsFragment extends Fragment {
    /* View references */
    private EditText mEditTextItemName;
    private EditText mEditTextDescription;
    private EditText mEditTextQuantity;
    private ImageButton mButtonIncrease;
    private ImageButton mButtonDecrease;
    private TextView mTextViewStatus;
    private Button mButtonEdit;
    private Space mSpace;
    private Button mButtonSave;

    private LineChart mLineChart;
    private TextView mTextViewHistoryLabel;

    private InventoryViewModel mInventoryViewModel;

    private Item mItem; // The item being displayed

    // onCreateView override method
    // Inflates the layout XML
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_item_details, container, false);
    }

    // onViewCreated override method
    // Handles setup after the view is created
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEditTextItemName = view.findViewById(R.id.editTextItemName);
        mEditTextDescription = view.findViewById(R.id.editTextDescription);
        mEditTextQuantity = view.findViewById(R.id.editTextQuantity);
        mButtonIncrease = view.findViewById(R.id.buttonIncrease);
        mButtonDecrease = view.findViewById(R.id.buttonDecrease);
        mButtonIncrease.setEnabled(false); // Start with increase/decrease buttons disabled
        mButtonDecrease.setEnabled(false);
        mTextViewStatus = view.findViewById(R.id.textViewStatus);
        mButtonEdit = view.findViewById(R.id.buttonEdit);
        mSpace = view.findViewById(R.id.space);
        Button buttonDelete = view.findViewById(R.id.buttonDelete);
        mButtonSave = view.findViewById(R.id.buttonSave);
        mLineChart = view.findViewById(R.id.lineChartHistory);
        mTextViewHistoryLabel = view.findViewById(R.id.textViewHistoryLabel);

        // Gets InventoryViewModel
        // ItemRepository is injected into it by Hilt
        // The ViewModel is scoped to only this fragment and
        // item LiveData is shared between fragments through ItemRepository
        mInventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Get the item ID passed from InventoryGridFragment through navigation arguments
        // Set itemId with this value or null if not found
        String itemId = getArguments() != null ? getArguments().getString("itemId") : null;

        /* LiveData observers */

        // getItem observer
        // Observes changes to the item LiveData
        // Calls displayItem to update UI when it changes
        // Navigates back to InventoryGridFragment if the item wasn't found
        mInventoryViewModel.getItem(itemId).observe(getViewLifecycleOwner(), item -> {
            if (item == null || itemId == null) {
                Navigation.findNavController(requireView()).popBackStack();
                return;
            }

            mItem = item;
            displayItem();
        });

        // getQuantityHistory observer
        // Observes changes to the item's QuantityHistory LiveData
        // Calls updateChart to update the chart when it changes
        if (itemId != null) { // Skip if item wasn't found
            mInventoryViewModel.getQuantityHistory(itemId)
                    .observe(getViewLifecycleOwner(), this::updateChart);
        }

        // getLowStockItemName observer
        // Observes changes to the LowStockItemName LiveData
        // Calls sendLowStockNotification to send a notification when it updates
        mInventoryViewModel.getLowStockItemName().observe(getViewLifecycleOwner(), itemName -> {
            if (itemName != null) {
                NotificationHelper.sendLowStockNotification(requireContext(), itemName);
                mInventoryViewModel.resetLowStockItemName();
            }
        });

        // getOperationResult observer
        // Observes changes to the OperationResult LiveData
        // Calls processResult to handle the OperationResult when it updates
        mInventoryViewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) { // Skip if result is null (fragment first created)
                processResult(result);
            }
        });

        /* Button listeners */

        // Increase button listener
        // Increments the quantity field by 1 in the UI only
        // Sets quantity to 1 if the field is empty or parsing the quantity string fails
        mButtonIncrease.setOnClickListener(v -> {
            // Clear error message
            mTextViewStatus.setText("");
            mTextViewStatus.setVisibility(TextView.GONE);

            // Set to 1 if field is empty
            if (mEditTextQuantity.getText().toString().trim().isEmpty()) {
                mEditTextQuantity.setText("1");
                return;
            }

            // Convert quantity string to integer before operation
            // Set to 1 if parseQuantity fails (returns -1)
            int quantity = ParseIntHelper.parseQuantity(mEditTextQuantity.getText().toString());
            if (quantity == -1) {
                mEditTextQuantity.setText("1");
                return;
            }

            // Increment quantity by 1 and set the EditText field with the new value
            mEditTextQuantity.setText(String.valueOf(quantity + 1));
        });

        // Decrease button listener
        // Decrements the quantity field by 1 in the UI only
        // Sets quantity to 0 if the field is empty or parsing the quantity string fails
        // Displays error message if the quantity is already 0
        mButtonDecrease.setOnClickListener(v -> {
            // Set to 0 if field is empty
            if (mEditTextQuantity.getText().toString().trim().isEmpty()) {
                mEditTextQuantity.setText("0");
                return;
            }

            // Convert quantity string to integer before operation
            // Set to 0 if parseQuantity fails (returns -1)
            int quantity = ParseIntHelper.parseQuantity(mEditTextQuantity.getText().toString());
            if (quantity == -1) {
                mEditTextQuantity.setText("0");
                return;
            }

            if (quantity > 0) { // Decrement if quantity is greater than 0
                mEditTextQuantity.setText(String.valueOf(quantity - 1));
                // Clear error message
                mTextViewStatus.setText("");
                mTextViewStatus.setVisibility(TextView.GONE);
            } else { // Display error message if quantity is already 0
                mTextViewStatus.setText(R.string.cannot_decrease);
                mTextViewStatus.setVisibility(TextView.VISIBLE);
            }
        });

        // Edit button listener
        // Call editMode to enable fields for editing
        mButtonEdit.setOnClickListener(v -> editMode());

        // Save button listener
        // Retrieves name, description, and quantity from EditText fields
        // Calls updateItem method in InventoryViewModel with these values
        mButtonSave.setOnClickListener(v -> {
            if (mItem == null) return; // Null check in case item was deleted elsewhere

            String name = mEditTextItemName.getText().toString().trim();
            String description = mEditTextDescription.getText().toString().trim();
            String quantityStr = mEditTextQuantity.getText().toString().trim();
            mInventoryViewModel.updateItem(mItem, name, description, quantityStr);
        });

        // Delete button listener
        // Displays a confirmation dialog before deleting the item
        // If user selects delete, the item is deleted, if they select cancel, the dialogue closes
        // Calls deleteItem method in InventoryViewModel with the item
        buttonDelete.setOnClickListener(v -> {
            if (mItem == null) return; // Null check in case item was deleted elsewhere
            // Confirmation dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete " + mItem.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) ->
                            mInventoryViewModel.deleteItem(mItem)) // Delete the item
                    .setNegativeButton("Cancel", null) // Close the dialog and do nothing
                    .create()
                    .show();
        });
    }

    // onDestroyView override method
    // Stops the Item and QuantityHistory listeners when the fragment is destroyed
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mInventoryViewModel.stopItemListener();
        mInventoryViewModel.stopQuantityHistoryListener();
    }

    // displayItem method
    // Updates the UI fields with the item's name, description, and quantity
    // Called by getItem observer when the item changes
    private void displayItem() {
        mEditTextItemName.setText(mItem.getName());
        mEditTextDescription.setText(mItem.getDescription());
        mEditTextQuantity.setText(String.valueOf(mItem.getQuantity()));
    }

    // processResult method
    // Takes an OperationResult code as a parameter
    // Called by the getOperationResult observer
    // to handle the OperationResult code when it updates
    // Calls viewMode to disable editing on successful update
    // Navigates back to InventoryGridFragment on successful delete
    // Shows error message otherwise
    private void processResult(InventoryViewModel.OperationResult result) {
        switch (result) {
            case SUCCESS:
            case SUCCESS_LOW_STOCK:
                viewMode();
                break;
            case SUCCESS_DELETE:
                Navigation.findNavController(requireView()).popBackStack();
                break;
            case EMPTY_FIELDS:
                mTextViewStatus.setText(R.string.update_item_empty);
                mTextViewStatus.setVisibility(TextView.VISIBLE);
                break;
            case INVALID_QUANTITY:
                mTextViewStatus.setText(R.string.invalid_quantity);
                mTextViewStatus.setVisibility(TextView.VISIBLE);
                break;
            case UPDATE_FAILED:
            default:
                mTextViewStatus.setText(R.string.update_item_failed);
                mTextViewStatus.setVisibility(TextView.VISIBLE);
                break;
        }
    }

    /* UI state helpers */

    // editMode method
    // Enables fields for editing when edit button is clicked
    // Shows save button and hides edit button
    private void editMode() {
        mEditTextItemName.setEnabled(true);
        mEditTextDescription.setEnabled(true);
        mEditTextQuantity.setEnabled(true);
        mButtonIncrease.setEnabled(true);
        mButtonDecrease.setEnabled(true);

        mButtonSave.setVisibility(Button.VISIBLE);
        mButtonEdit.setVisibility(Button.GONE);

        mSpace.setVisibility(Space.GONE);
    }

    // viewMode method
    // Disables editing fields after a successful update
    // Hides save button and shows edit button
    // Clears and hides error message
    private void viewMode() {
        mTextViewStatus.setText("");
        mTextViewStatus.setVisibility(TextView.GONE);

        mEditTextItemName.setEnabled(false);
        mEditTextDescription.setEnabled(false);
        mEditTextQuantity.setEnabled(false);
        mButtonIncrease.setEnabled(false);
        mButtonDecrease.setEnabled(false);

        mButtonSave.setVisibility(Button.GONE);
        mButtonEdit.setVisibility(Button.VISIBLE);

        mSpace.setVisibility(Space.VISIBLE);
    }

    // updateChart method
    // Uses MPAndroid line chart to display quantity changes over time
    // Takes list of the item's QuantityHistory objects as a parameter
    // If there is no history, the chart is hidden
    // Otherwise, the chart is built and displayed
    private void updateChart(List<QuantityHistory> historyList) {
        // If there is no history, hide the chart
        if (historyList == null || historyList.isEmpty()) {
            mLineChart.setVisibility(View.GONE);
            mTextViewHistoryLabel.setVisibility(View.GONE);
            return;
        }

        // Show the chart
        mLineChart.setVisibility(View.VISIBLE);
        mTextViewHistoryLabel.setVisibility(View.VISIBLE);

        // Iterate over every item in the history list to build the data points
        List<Entry> entries = new ArrayList<>();
        List<Date> timestamps = new ArrayList<>();

        for (int i = 0; i < historyList.size(); i++) {
            QuantityHistory entry = historyList.get(i);
            entries.add(new Entry(i, entry.getNewQuantity()));
            timestamps.add(entry.getTimestamp());
        }

        // Set line styling
        LineDataSet dataSet = new LineDataSet(entries, "Quantity");
        dataSet.setColor(Color.parseColor("#6200EE"));
        dataSet.setCircleColor(Color.parseColor("#6200EE"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        // Set the chart data
        mLineChart.setData(new LineData(dataSet));
        mLineChart.getDescription().setEnabled(false); // Hides description label
        mLineChart.getLegend().setEnabled(false); // Hides legend
        mLineChart.setTouchEnabled(true); // Allows interaction
        mLineChart.setDragEnabled(true); // Allows dragging

        // Configure the x-axis with dates along the bottom
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Sets a minimum of 1 unit between labels
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.US);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timestamps.size()) {
                    return sdf.format(timestamps.get(index));
                }
                return "";
            }
        });

        // Configure y-axis with quantity values along the left side
        mLineChart.getAxisRight().setEnabled(false);
        mLineChart.getAxisLeft().setGranularity(1f);

        // Redraws the chart with new data
        mLineChart.invalidate();
    }
}
