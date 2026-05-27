package com.bencobble.inventorytracker.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bencobble.inventorytracker.repository.UserRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

// LoginViewModel used by LoginFragment
// Connects to UserRepository for Firebase Authentication operations
// Exposes UserResult MutableLiveData observed by LoginFragment
// Managed by Hilt for dependency injection
@HiltViewModel
public class LoginViewModel extends ViewModel {
    private final UserRepository mRepo;

    // UserResult enum
    // Contains status codes for login and account creation
    public enum UserResult {
        SUCCESS_LOGIN,
        SUCCESS_CREATE_ACC,
        EMPTY_FIELDS,
        INVALID_CREDENTIALS,
        USER_NOT_FOUND,
        USER_DISABLED,
        TOO_MANY_REQUESTS,
        EMAIL_TAKEN,
        EMAIL_INVALID,
        WEAK_PASSWORD,
        FIREBASE_AUTH_ERROR,
        AUTHENTICATION_DISABLED
    }

    // LiveData to hold the UserResult enum status code from login or account creation
    private final MutableLiveData<UserResult> mUserResult = new MutableLiveData<>();
    public LiveData<UserResult> getUserResult() { return mUserResult; }

    /* Constructor */
    @Inject
    public LoginViewModel(UserRepository repo) {
        mRepo = repo;
    }

    /* Authentication methods */

    // login method
    // Takes an email and password as parameters
    // Calls UserRepository login method to attempt login
    public void login(String email, String password) {
        // Check for empty fields before calling login
        if (email.isEmpty() || password.isEmpty()) {
            mUserResult.setValue(UserResult.EMPTY_FIELDS);
            return;
        }
        mRepo.login(email, password, mUserResult);
    }

    // createAccount method
    // Takes an email and password as parameters
    // Calls UserRepository createAccount method to attempt account creation
    public void createAccount(String email, String password) {
        // Check for empty fields before calling createAccount
        if (email.isEmpty() || password.isEmpty()) {
            mUserResult.setValue(UserResult.EMPTY_FIELDS);
            return;
        }
        mRepo.createAccount(email, password, mUserResult);
    }

    // isLoggedIn method
    // Returns true if a user is logged in, false otherwise
    // Calls UserRepository isLoggedIn method
    public boolean isLoggedIn() {
        return mRepo.isLoggedIn();
    }
}
