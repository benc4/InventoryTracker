package com.bencobble.inventorytracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

// InventoryItemAdapter class
// Handles binding item data to cards in the RecyclerView displayed in InventoryGridFragment
// Extends ListAdapter which provides efficient methods for updating the RecyclerView
public class InventoryItemAdapter extends ListAdapter<Item, InventoryItemAdapter.InventoryItemHolder> {
    // DIFF_CALLBACK
    // Tells DiffUtil how to compare items
    // areItemsTheSame compares items by their IDs
    // areContentsTheSame compares items by their contents
    // Both methods return true if items are the same and false otherwise
    // Used to determine which items should be updated by onBindViewHolder
    private static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.getID() != null && oldItem.getID().equals(newItem.getID());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getDescription().equals(newItem.getDescription())
                    && oldItem.getQuantity() == newItem.getQuantity();
        }
    };

    private final InventoryViewModel mViewModel;
    private final OnItemListener mListener;

    // OnItemListener interface
    // Allows InventoryGridFragment to handle clicking on cards
    public interface OnItemListener {
        void onItemClick(Item item);
    }

    /* Constructor */
    public InventoryItemAdapter(InventoryViewModel viewModel, OnItemListener listener) {
        super(DIFF_CALLBACK);
        mViewModel = viewModel;
        mListener = listener;
    }

    // onCreateViewHolder override method
    // Inflates the layout XML for the cards
    // Called only for items that will fit on the page in the RecyclerView,
    // cards are recycled and reused when scrolling
    @NonNull
    @Override
    public InventoryItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_items, parent, false);
        return new InventoryItemHolder(view);
    }

    // onBindViewHolder override method
    // Binds UI elements and item data to a card at a given position in the RecyclerView
    // Uses getItem to get the item at the given position and holder.bind to bind the data
    @Override
    public void onBindViewHolder(@NonNull InventoryItemHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // InventoryItemHolder inner class
    // Holds references to the view elements inside a single card
    class InventoryItemHolder extends RecyclerView.ViewHolder {
        // View references
        private final TextView mNameTextView;
        private final TextView mQuantityTextView;
        private final ImageButton mIncreaseButton;
        private final ImageButton mDecreaseButton;
        private final ImageButton mDeleteButton;

        // Sets up the cards
        public InventoryItemHolder(@NonNull View itemView) {
            super(itemView);
            mNameTextView = itemView.findViewById(R.id.item_text_view);
            mQuantityTextView = itemView.findViewById(R.id.item_quantity_text_view);
            mIncreaseButton = itemView.findViewById(R.id.buttonIncrease);
            mDecreaseButton = itemView.findViewById(R.id.buttonDecrease);
            mDeleteButton = itemView.findViewById(R.id.buttonDelete);
        }

        // bind method
        // Populates the card with data and sets up click listeners
        public void bind(Item item) {
            // Click listener for the card itself
            // Calls onItemClick which is overridden by InventoryGridFragment
            // to navigate to ItemDetailsFragment
            itemView.setOnClickListener(view -> mListener.onItemClick(item));

            // Display item name and quantity
            mNameTextView.setText(item.getName());
            mQuantityTextView.setText(itemView.getContext().getString(R.string.qty) + item.getQuantity());

            // Increase and decrease button listeners
            // Calls increase/decrease methods in InventoryViewModel for +/- 1 quantity
            mIncreaseButton.setOnClickListener(view -> mViewModel.increaseQuantity(item));
            mDecreaseButton.setOnClickListener(view -> mViewModel.decreaseQuantity(item));

            // Delete button listener
            // Displays a confirmation dialog before deleting the item
            // If user selects delete, the item is deleted, if they select cancel, the dialogue closes
            // Calls deleteItem method in InventoryViewModel with the item
            mDeleteButton.setOnClickListener(view ->
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete Item")
                            .setMessage("Are you sure you want to delete " + item.getName() + "?")
                            .setPositiveButton("Delete", (dialog, which) ->
                                    mViewModel.deleteItem(item)) // Delete the item
                            .setNegativeButton("Cancel", null) // Close the dialog and do nothing
                            .create()
                            .show());
        }
    }
}
