package com.bencobble.inventorytracker.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.adapter.InventoryItemAdapter;
import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.repository.UserRepository;
import com.bencobble.inventorytracker.util.NotificationHelper;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

// InventoryGridFragment
// Main screen of the app
// Displays all items in a grid
// Handles user input and UI for navigation options,
// searching, and using increase/decrease/delete buttons on item cards
// Also handles notifications for low stock items
// Implements OnItemListener so the adapter can call back when an item card is clicked
// Dependency injection managed by Hilt
@AndroidEntryPoint
public class InventoryGridFragment extends Fragment
        implements InventoryItemAdapter.OnItemListener {

    // UserRepository is injected directly to handle logging out
    @Inject
    UserRepository mUserRepository;

    private InventoryViewModel mInventoryViewModel;

    // Notification permission launcher
    // Uses the Activity Result API to determine if permissions have been granted
    private final ActivityResultLauncher<String> mNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    // onCreateView override method
    // Inflates the layout XML
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_inventory_grid, container, false);
    }

    // onViewCreated override method
    // Handles setup after the view is created
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Gets InventoryViewModel
        // ItemRepository is injected into it by Hilt
        // The ViewModel is scoped to only this fragment and
        // item LiveData is shared between fragments through ItemRepository
        mInventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Sets up RecyclerView for the card grid layout with 2 columns
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewInventory);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // Gets and sets the adapter for the RecyclerView to bind items to the cards
        InventoryItemAdapter adapter = new InventoryItemAdapter(mInventoryViewModel, this);
        recyclerView.setAdapter(adapter);

        // getItems observer
        // Observes changes to the mFilteredItems LiveData
        // Calls submitList in the adapter to update the RecyclerView when it updates
        mInventoryViewModel.getItems().observe(getViewLifecycleOwner(), adapter::submitList);

        // getOperationResult observer
        // Observes changes to the OperationResult LiveData
        // Calls processResult to handle the OperationResult when it updates
        mInventoryViewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                processResult(result);
            }
        });

        // getLowStockItemName observer
        // Observes changes to the LowStockItemName LiveData
        // Calls sendLowStockNotification to send a notification when it updates
        mInventoryViewModel.getLowStockItemName().observe(getViewLifecycleOwner(), itemName -> {
            if (itemName != null) {
                NotificationHelper.sendLowStockNotification(requireContext(), itemName);
                mInventoryViewModel.resetLowStockItemName();
            }
        });

        // Requests notification permissions
        requestNotificationPermission();

        // Add Item floating action button listener
        // Navigates to AddItemFragment when clicked
        view.findViewById(R.id.fabAddItem).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_grid_to_addItem));

        // Menu provider for the toolbar with search and sign out options
        requireActivity().addMenuProvider(new MenuProvider() {
            // onCreateMenu override method
            // Inflates the menu XML and sets up the SearchView
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.inventory_menu, menu); // Inflate menu

                // Inflate and set up search bar
                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();
                searchView.setQueryHint("Search items");

                // SearchView listener
                // Listens for text changes in the search bar
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

                // SearchView expand/collapse listener
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
            }

            // onMenuItemSelected override method
            // Listener for the sign out button in app bar menu
            // Displays a confirmation dialog before signing out
            // If user selects sign out, signOut is called and it navigates to LoginFragment
            // If they select cancel, the dialogue closes and nothing happens
            // Returns true to indicate that clicking the menu option has been handled
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_sign_out) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Sign Out")
                            .setMessage(R.string.sign_out_confirm)
                            .setPositiveButton("Sign Out", (dialog, which) -> {
                                mUserRepository.signOut();
                                Navigation.findNavController(requireView())
                                        .navigate(R.id.action_grid_to_login);
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    // processResult method
    // Takes an OperationResult code as a parameter
    // Called by the getOperationResult observer
    // to handle the OperationResult code when it updates
    // Displays status message based on the result code
    private void processResult(InventoryViewModel.OperationResult result) {
        switch (result) {
            case SUCCESS:
            case SUCCESS_LOW_STOCK:
            case SUCCESS_DELETE:
                break;
            case UPDATE_FAILED:
                Toast.makeText(requireContext(), R.string.update_item_failed, Toast.LENGTH_SHORT).show();
                break;
            case CANNOT_DECREASE:
                Toast.makeText(requireContext(), R.string.cannot_decrease, Toast.LENGTH_SHORT).show();
                break;
            case DELETE_FAILED:
                Toast.makeText(requireContext(), R.string.delete_item_failed, Toast.LENGTH_SHORT).show();
                break;
            case GET_ITEMS_FAILED:
                Toast.makeText(requireContext(), R.string.get_items_failed, Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // requestNotificationPermission method
    // Requests notification permissions
    // Checks if running on API 33 (Tiramisu) and higher since older versions don't require it
    // If version is 33+ and permissions are not yet granted, the permissions are requested
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                mNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // onItemClick override method
    // Called when an item card is clicked
    // Overridden from the OnItemListener interface in InventoryItemAdapter,
    // which allows the adapter to call back when an item card is clicked
    // Navigates to ItemDetailsFragment
    // Passes the item's ID through a Bundle in the navigation arguments,
    // which is used by ItemDetailsFragment to retrieve the item's data
    @Override
    public void onItemClick(Item item) {
        Bundle bundle = new Bundle();
        bundle.putString("itemId", item.getID());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_grid_to_itemDetails, bundle);
    }
}
