package com.bencobble.inventorytracker.repository;

import androidx.lifecycle.MutableLiveData;

import android.util.Log;

import com.bencobble.inventorytracker.viewmodel.LoginViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;
import javax.inject.Singleton;

// UserRepository class
// Handles Firebase authentication operations
// Singleton instance managed by Hilt
@Singleton
public class UserRepository {
    private final FirebaseAuth mAuth;

    /*Constructor*/
    @Inject
    public UserRepository() {
        mAuth = FirebaseAuth.getInstance();
    }

    /*Authentication methods*/

    // login method
    // Takes email, password, and UserResult LiveData as parameters
    // Attempts to sign in with the provided credentials
    // Uses Firebase Authentication's signInWithEmailAndPassword method
    // Posts result as a status from UserResult to the LiveData,
    // based on Firebase Authentication error codes
    public void login(String email, String password, MutableLiveData<LoginViewModel.UserResult> result) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) { // Login successful
                        result.postValue(LoginViewModel.UserResult.SUCCESS_LOGIN);
                    } else { // Login failed
                        // Get the error message from the task
                        Exception e = task.getException();
                        String errorCode = (e instanceof FirebaseAuthException)
                                ? ((FirebaseAuthException) e).getErrorCode() : "";

                        // Convert Firebase Auth error code to UserResult code
                        // Post translated code to UserResult LiveData
                        switch (errorCode) {
                            case "ERROR_USER_NOT_FOUND": // Using an email without account just throws ERROR_INVALID_CREDENTIAL
                                result.postValue(LoginViewModel.UserResult.USER_NOT_FOUND);
                                break;
                            case "ERROR_WRONG_PASSWORD": // Email exists but password is wrong
                            case "ERROR_INVALID_CREDENTIAL":
                                result.postValue(LoginViewModel.UserResult.INVALID_CREDENTIALS);
                                break;
                            case "ERROR_USER_DISABLED": // User account disabled from the Firebase console
                                result.postValue(LoginViewModel.UserResult.USER_DISABLED);
                                break;
                            case "ERROR_INVALID_EMAIL": // Email is not formatted correctly
                                result.postValue(LoginViewModel.UserResult.EMAIL_INVALID);
                                break;
                            default:
                                Log.e("UserRepository", "Login failed: " + errorCode, e);
                                result.postValue(LoginViewModel.UserResult.FIREBASE_AUTH_ERROR);
                                break;
                        }
                    }
                });
    }

    // createAccount method
    // Takes email, password, and UserResult LiveData as parameters
    // Attempts to create an account with the provided credentials
    // Uses Firebase Authentication's createUserWithEmailAndPassword method
    // Posts result as a status from UserResult to the LiveData,
    // based on Firebase Authentication error codes
    public void createAccount(String email, String password, MutableLiveData<LoginViewModel.UserResult> result) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) { // Account creation successful
                        mAuth.signOut();
                        result.postValue(LoginViewModel.UserResult.SUCCESS_CREATE_ACC);
                    } else { // Account creation failed
                        // Get the error message from the task
                        Exception e = task.getException();
                        String errorCode = (e instanceof FirebaseAuthException)
                                ? ((FirebaseAuthException) e).getErrorCode() : "";

                        // Convert Firebase Auth error code to UserResult code
                        // Post translated code to UserResult LiveData
                        switch (errorCode) {
                            case "ERROR_EMAIL_ALREADY_IN_USE": // Email taken
                                result.postValue(LoginViewModel.UserResult.EMAIL_TAKEN);
                                break;
                            case "ERROR_INVALID_EMAIL": // Email is not formatted correctly
                                result.postValue(LoginViewModel.UserResult.EMAIL_INVALID);
                                break;
                            case "ERROR_WEAK_PASSWORD": // Password is too weak
                                result.postValue(LoginViewModel.UserResult.WEAK_PASSWORD);
                                break;
                            case "ERROR_TOO_MANY_REQUESTS": // Too many requests made in quick succession
                                result.postValue(LoginViewModel.UserResult.TOO_MANY_REQUESTS);
                                break;
                            case "ERROR_OPERATION_NOT_ALLOWED": // Email/password authentication is disabled
                            case "ERROR_ADMIN_RESTRICTED_OPERATION": // Account creation is disabled
                                result.postValue(LoginViewModel.UserResult.AUTHENTICATION_DISABLED);
                                break;
                            default:
                                Log.e("UserRepository", "Account Creation failed: " + errorCode, e);
                                result.postValue(LoginViewModel.UserResult.FIREBASE_AUTH_ERROR);
                                break;
                        }
                    }
                });
    }

    /* Other user methods */

    // getCurrentUserId method
    // Returns the uid of the currently logged in user,
    // null if no user is logged in
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // isLoggedIn method
    // Returns true if a user is logged in, false otherwise
    // Calls getCurrentUser Firebase method to check if a user is logged in
    public boolean isLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // signOut method
    // Signs the current user out of the app
    public void signOut() {
        mAuth.signOut();
    }
}
