package com.bencobble.inventorytracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.lifecycle.ViewModelProvider;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.viewmodel.LoginViewModel;

// Login screen
public class LoginActivity extends AppCompatActivity {
    private EditText mEditTextUsername;
    private EditText mEditTextPassword;
    private TextView mTextViewStatus;
    private Button mButtonLogin;
    private Button mButtonCreateAccount;

    // Spinner that only displays after 500ms to prevent flashing on quick operations
    private ContentLoadingProgressBar mProgressSpinner;

    private LoginViewModel mLoginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        mEditTextUsername = findViewById(R.id.editTextUsername);
        mEditTextPassword = findViewById(R.id.editTextPassword);
        mTextViewStatus = findViewById(R.id.textViewStatus);
        mButtonLogin = findViewById(R.id.buttonLogin);
        mButtonCreateAccount = findViewById(R.id.buttonCreateAccount);
        mProgressSpinner = findViewById(R.id.loginProgressSpinner);

        // Create LoginViewModel
        mLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Observe changes to mUserResult after login or createAccount
        mLoginViewModel.getUserResult().observe(this, result -> {
            if (result != null) { // Don't run when activity is created
                processResult(result);
            }
        });

        // Login button
        mButtonLogin.setOnClickListener(view -> {
            // Get username and password from EditText
            String username = mEditTextUsername.getText().toString().trim();
            String password = mEditTextPassword.getText().toString().trim();

            // Disable UI while processing
            disableLoginUI();

            // Call login method in LoginViewModel
            mLoginViewModel.login(username, password);
        });

        // Create Account Button
        mButtonCreateAccount.setOnClickListener(view -> {
            // Get username and password from EditText fields
            String username = mEditTextUsername.getText().toString().trim();
            String password = mEditTextPassword.getText().toString().trim();

            // Disable UI while processing
            disableLoginUI();

            // Call createAccount method in LoginViewModel
            mLoginViewModel.createAccount(username, password);
        });
    }

    // Handles result code from mUserResult
    private void processResult(LoginViewModel.UserResult result) {
        // Successful login starts InventoryGridActivity
        if (result == LoginViewModel.UserResult.SUCCESS_LOGIN) {
            Intent intent = new Intent(this, InventoryGridActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Show status message
        showStatus(result);

        // Re-enable UI elements after processing
        enableLoginUI();
    }

    // Shows status message in mTextViewStatus based on result code
    private void showStatus(LoginViewModel.UserResult result) {
        String message = "";

        // Update message based on result code
        switch (result) {
            case SUCCESS_CREATE_ACC:
                message = (getString(R.string.account_created));
                break;
            case EMPTY_FIELDS:
                message = (getString(R.string.enter_user_pass));
                break;
            case USERNAME_TAKEN:
                message = (getString(R.string.username_exists));
                break;
            case INVALID_CREDENTIALS:
                message = (getString(R.string.invalid_user_pass));
                break;
            case USER_NOT_FOUND:
                message = (getString(R.string.user_not_found));
                break;
            case DATABASE_ERROR:
                message = (getString(R.string.database_error));
                break;
        }

        // Set status message
        mTextViewStatus.setText(message);

        // Set text color based on success
        // Uses a typedValue to get the color based on the theme
        TypedValue typedValue = new TypedValue();
        if (result == LoginViewModel.UserResult.SUCCESS_CREATE_ACC) { // Green text
            getTheme().resolveAttribute(R.attr.textColorSuccess, typedValue, true);
        } else { // Red text
            getTheme().resolveAttribute(R.attr.textColorError, typedValue, true);
        }
        mTextViewStatus.setTextColor(typedValue.data);

        // Show status
        mTextViewStatus.setVisibility(TextView.VISIBLE);
    }

    // Disables UI elements
    private void disableLoginUI() {
        // Disable EditText fields and Buttons
        mEditTextUsername.setEnabled(false);
        mEditTextPassword.setEnabled(false);
        mButtonLogin.setEnabled(false);
        mButtonCreateAccount.setEnabled(false);

        // Clear error message
        mTextViewStatus.setText("");
        mTextViewStatus.setVisibility(TextView.GONE);

        // Show progress spinner
        mProgressSpinner.show();
    }

    // Enables UI elements
    private void enableLoginUI() {
        // Enable EditText fields and Buttons
        mEditTextUsername.setEnabled(true);
        mEditTextPassword.setEnabled(true);
        mButtonLogin.setEnabled(true);
        mButtonCreateAccount.setEnabled(true);

        // Hide progress spinner
        mProgressSpinner.hide();
    }
}