package com.bencobble.inventorytracker.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.bencobble.inventorytracker.R;

import dagger.hilt.android.AndroidEntryPoint;

// MainActivity
// Serves as the single activity for the app
// Hosts the NavHostFragment for navigation between the fragments
// Dependency injection managed by Hilt
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    // onCreate override method
    // Sets up the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Boilerplate onCreate code for setting up EdgeToEdge display and
        // WindowInsets to prevent EdgeToEdge from rendering behind system bars
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sets up the NavHostFragment which holds the fragment currently being displayed
        // The navigation graph defined in nav_graph.xml defines the transitions between fragments
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        // Connects the navController to the action bar's title and back button
        NavigationUI.setupActionBarWithNavController(this, navController);
    }

    // onSupportNavigateUp override method
    // Handles the back button in the action bar
    // Tries NavController.navigateUp() first, which pops the back stack
    // If navigateUp fails (already at the root fragment), falls back to super.onSupportNavigateUp()
    // which uses default activity behavior
    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
