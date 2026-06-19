package com.bencobble.inventorytracker.fragments;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.viewmodel.LoginViewModel;

import dagger.hilt.android.AndroidEntryPoint;

// LoginFragment
// Handles user input and UI for the login screen
// Dependency injection managed by Hilt
@AndroidEntryPoint
public class LoginFragment extends Fragment {
    /* View references */
    private EditText mEditTextEmail;
    private EditText mEditTextPassword;
    private TextView mTextViewStatus;
    private Button mButtonLogin;
    private Button mButtonCreateAccount;
    private ContentLoadingProgressBar mProgressSpinner;

    private LoginViewModel mLoginViewModel;

    // onCreateView override method
    // Inflates the layout XML
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_login, container, false);
    }

    // onViewCreated override method
    // Handles setup after the view is created
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEditTextEmail = view.findViewById(R.id.editTextEmail);
        mEditTextPassword = view.findViewById(R.id.editTextPassword);
        mTextViewStatus = view.findViewById(R.id.textViewStatus);
        mButtonLogin = view.findViewById(R.id.buttonLogin);
        mButtonCreateAccount = view.findViewById(R.id.buttonCreateAccount);
        mProgressSpinner = view.findViewById(R.id.loginProgressSpinner);

        // Gets LoginViewModel
        // UserRepository is injected into it by Hilt
        mLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Check if the user is already logged in
        // If they are, load their orgId and route to grid or org setup
        if (mLoginViewModel.isLoggedIn()) {
            disableLoginUI();
            mLoginViewModel.loadCurrentOrgId(this::routeAfterAuth);
            return;
        }

        // getUserResult observer
        // Observes changes to the UserResult LiveData
        // Calls processResult to handle the UserResult when it updates
        mLoginViewModel.getUserResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) { // Skip if result is null (fragment first created)
                processResult(result);
            }
        });

        // Login button listener
        // Retrieves email and password from EditText fields
        // Calls login method in LoginViewModel with them
        mButtonLogin.setOnClickListener(v -> {
            String email = mEditTextEmail.getText().toString().trim();
            String password = mEditTextPassword.getText().toString().trim();
            disableLoginUI(); // Disable UI while login is in progress
            mLoginViewModel.login(email, password);
        });

        // Create Account button listener
        // Retrieves email and password from EditText fields
        // Calls createAccount method in LoginViewModel with them
        mButtonCreateAccount.setOnClickListener(v -> {
            String email = mEditTextEmail.getText().toString().trim();
            String password = mEditTextPassword.getText().toString().trim();
            disableLoginUI(); // Disable UI while creation is in progress
            mLoginViewModel.createAccount(email, password);
        });
    }

    // processResult method
    // Takes a UserResult code as a parameter
    // Called by the getUserResult observer
    private void processResult(LoginViewModel.UserResult result) {
        if (result == LoginViewModel.UserResult.SUCCESS_LOGIN) {
            mLoginViewModel.loadCurrentOrgId(this::routeAfterAuth);
            return;
        }

        if (result == LoginViewModel.UserResult.SUCCESS_CREATE_ACC) {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_login_to_orgSetup);
            return;
        }

        showStatus(result);
        enableLoginUI(); // Re-enable UI because processing is done
    }

    // routeAfterAuth method
    // Reads the orgId and navigates to grid or org setup
    // Called after loadCurrentOrgId completes
    private void routeAfterAuth() {
        if (mLoginViewModel.getCurrentOrgId() != null) {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_login_to_grid);
        } else {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_login_to_orgSetup);
        }
    }

    // showStatus method
    // Takes UserResult result code as a parameter
    // Uses a switch statement to set the status text
    // based on the result code
    private void showStatus(LoginViewModel.UserResult result) {
        String message = "";

        switch (result) {
            case SUCCESS_CREATE_ACC:
                message = getString(R.string.account_created);
                break;
            case EMPTY_FIELDS:
                message = getString(R.string.enter_user_pass);
                break;
            case EMAIL_TAKEN:
                message = getString(R.string.username_exists);
                break;
            case INVALID_CREDENTIALS:
                message = getString(R.string.invalid_user_pass);
                break;
            case USER_NOT_FOUND:
                message = getString(R.string.user_not_found);
                break;
            case USER_DISABLED:
                message = "User account is disabled.";
                break;
            case FIREBASE_AUTH_ERROR:
                message = "An error occurred with Firebase authentication. Please try again.";
                break;
            case EMAIL_INVALID:
                message = "Please enter a valid email address.";
                break;
            case WEAK_PASSWORD:
                message = "Password is too weak. Please choose a stronger password and try again.";
                break;
            case TOO_MANY_REQUESTS:
                message = "You have been temporarily blocked due to too many failed attempts. Try again later.";
                break;
            case AUTHENTICATION_DISABLED:
                message = "Authentication has been disabled. Please try again later.";
                break;
            default:
                message = "An unknown authentication error occurred. Please try again.";
                break;
        }

        // Set status text to the updated message
        mTextViewStatus.setText(message);

        // Set text color based on success or error using the color variant from the current theme
        TypedValue typedValue = new TypedValue();
        if (result == LoginViewModel.UserResult.SUCCESS_CREATE_ACC) { // Green success text
            requireContext().getTheme().resolveAttribute(R.attr.textColorSuccess, typedValue, true);
        } else { // Red error text
            requireContext().getTheme().resolveAttribute(R.attr.textColorError, typedValue, true);
        }
        mTextViewStatus.setTextColor(typedValue.data);

        // Show status text in the UI
        mTextViewStatus.setVisibility(TextView.VISIBLE);
    }

    /* UI state helpers */

    // disableLoginUI method
    // Disables the UI elements while login/creation is processing
    private void disableLoginUI() {
        mEditTextEmail.setEnabled(false);
        mEditTextPassword.setEnabled(false);
        mButtonLogin.setEnabled(false);
        mButtonCreateAccount.setEnabled(false);

        // Reset and hide status text
        mTextViewStatus.setText("");
        mTextViewStatus.setVisibility(TextView.GONE);

        // ContentLoadingProgressBar that displays after 500ms
        // if .hide() hasn't been called on it yet
        mProgressSpinner.show();
    }

    // enableLoginUI method
    // Enables the login UI after login/creation finishes processing
    private void enableLoginUI() {
        mEditTextEmail.setEnabled(true);
        mEditTextPassword.setEnabled(true);
        mButtonLogin.setEnabled(true);
        mButtonCreateAccount.setEnabled(true);

        // Hide the ContentLoadingProgressBar
        mProgressSpinner.hide();
    }
}
