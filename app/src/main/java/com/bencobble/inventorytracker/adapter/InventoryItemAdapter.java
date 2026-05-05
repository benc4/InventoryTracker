package com.bencobble.inventorytracker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

import java.util.ArrayList;
import java.util.List;

// Adapter class for Item cards in RecyclerView
public class InventoryItemAdapter extends RecyclerView.Adapter<InventoryItemAdapter.InventoryItemHolder> {
    private List<Item> mItems = new ArrayList<>();
    private final InventoryViewModel mViewModel;
    private final OnItemListener mListener;

    // Interface to handle clicking a card
    public interface OnItemListener {
        void onItemClick(Item item);
    }

    public InventoryItemAdapter(InventoryViewModel viewModel, OnItemListener listener) {
        mViewModel = viewModel;
        mListener = listener;
    }

    // Updates the list of items in the adapter
    public void setItems(List<Item> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    // Creates a new card in the RecyclerView
    @NonNull
    @Override
    public InventoryItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new InventoryItemHolder(inflater, parent);
    }

    // Updates an existing card in the RecyclerView
    @Override
    public void onBindViewHolder(@NonNull InventoryItemHolder holder, int position) {
        holder.bind(mItems.get(position));
    }

    // Sets the number of cards in the RecyclerView
    @Override
    public int getItemCount() {
        return mItems.size();
    }

    // Represents a single card in the grid
    class InventoryItemHolder extends RecyclerView.ViewHolder {
        private final TextView mNameTextView;
        private final TextView mQuantityTextView;
        private final ImageButton mIncreaseButton;
        private final ImageButton mDecreaseButton;
        private final ImageButton mDeleteButton;

        // Inflates the card layout
        public InventoryItemHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.recycler_view_items, parent, false));
            mNameTextView = itemView.findViewById(R.id.item_text_view);
            mQuantityTextView = itemView.findViewById(R.id.item_quantity_text_view);
            mIncreaseButton = itemView.findViewById(R.id.buttonIncrease);
            mDecreaseButton = itemView.findViewById(R.id.buttonDecrease);
            mDeleteButton = itemView.findViewById(R.id.buttonDelete);
        }

        // Binds item data to a card and sets up button listeners
        public void bind(Item item) {
            // Handles clicking a card
            // Calls onItemClick to launch ItemDetailsActivity
            itemView.setOnClickListener(view -> mListener.onItemClick(item));

            // Populates the item name and quantity in the card
            mNameTextView.setText(item.getName());
            mQuantityTextView.setText(itemView.getContext().getString(R.string.qty) + item.getQuantity());

            // Increase button
            mIncreaseButton.setOnClickListener(view -> mViewModel.increaseQuantity(item));

            // Decrease button
            mDecreaseButton.setOnClickListener(view -> mViewModel.decreaseQuantity(item));

            // Delete button
            mDeleteButton.setOnClickListener(view -> {
                // Show delete confirmation dialogue
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to delete " + item.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            mViewModel.deleteItem(item); // Delete item from database
                        })
                        .setNegativeButton("Cancel", null) // Do nothing on cancel
                        .create()
                        .show();
            });
        }
    }
}
