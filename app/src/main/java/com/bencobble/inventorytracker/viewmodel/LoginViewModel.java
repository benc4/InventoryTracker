package com.bencobble.inventorytracker.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bencobble.inventorytracker.repository.InventoryRepository;

// ViewModel for LoginActivity
// Connects to InventoryRepository for database operations
// Handles login and account creation
public class LoginViewModel extends AndroidViewModel {
    private final InventoryRepository mRepo;

    // Contains status codes for login and account creation
    public enum UserResult {
        SUCCESS_LOGIN,
        SUCCESS_CREATE_ACC,
        EMPTY_FIELDS,
        INVALID_CREDENTIALS,
        USER_NOT_FOUND,
        DATABASE_ERROR,
        USERNAME_TAKEN,
    }

    // LiveData to hold the result of login or account creation
    private final MutableLiveData<UserResult> mUserResult = new MutableLiveData<>();
    public LiveData<UserResult> getUserResult() { return mUserResult; }

    public LoginViewModel(Application application) {
        super(application);
        mRepo = InventoryRepository.getInstance(application.getApplicationContext());
    }

    // Attempts to log in using provided username and password
    public void login(String username, String password) {
        // Check if fields are empty before contacting db
        if (username.isEmpty() || password.isEmpty()) {
            mUserResult.setValue(UserResult.EMPTY_FIELDS);
            return;
        }
        // Call login method in InventoryRepository
        mRepo.login(username, password, mUserResult);
    }

    // Attempts to add a new user to the database
    // using provided username and password
    public void createAccount(String username, String password) {
        // Check if fields are empty before contacting db
        if (username.isEmpty() || password.isEmpty()) {
            mUserResult.setValue(UserResult.EMPTY_FIELDS);
            return;
        }
        // Call createAccount method in InventoryRepository
        mRepo.createAccount(username, password, mUserResult);
    }
}
