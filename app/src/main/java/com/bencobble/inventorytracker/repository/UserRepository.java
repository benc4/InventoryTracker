package com.bencobble.inventorytracker.repository;

import androidx.lifecycle.MutableLiveData;

import android.util.Log;

import com.bencobble.inventorytracker.model.Organization;
import com.bencobble.inventorytracker.viewmodel.LoginViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

// UserRepository class
// Handles Firebase authentication and organization operations
// Singleton instance managed by Hilt
@Singleton
public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;

    private String mCurrentOrgId = null;
    private String mLastCreatedInviteCode = null;

    /*Constructor*/
    @Inject
    public UserRepository() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
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

    /* Organization methods */

    // createOrganization method
    // Creates a new organization with the given name
    // Posts result as a status from UserResult to the LiveData
    public void createOrganization(String name, MutableLiveData<LoginViewModel.UserResult> result) {
        String uid = getCurrentUserId();

        // Name or uid null check
        if (uid == null) {
            result.postValue(LoginViewModel.UserResult.ORG_OPERATION_FAILED);
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            result.postValue(LoginViewModel.UserResult.ORG_NAME_EMPTY);
            return;
        }

        // Generate the organization's document reference and invite code
        DocumentReference orgRef = mFirestore.collection("organizations").document();
        String orgId = orgRef.getId();
        String inviteCode = generateInviteCode();
        Organization org = new Organization(name.trim(), uid, inviteCode);

        // Write the new organization with a Firestore WriteBatch
        WriteBatch batch = mFirestore.batch();
        batch.set(orgRef, org);
        Map<String, Object> userData = new HashMap<>();
        userData.put("organizationId", orgId);
        batch.set(mFirestore.collection("users").document(uid), userData, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    mCurrentOrgId = orgId;
                    mLastCreatedInviteCode = inviteCode;
                    result.postValue(LoginViewModel.UserResult.SUCCESS_ORG_SETUP);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating organization: " + e.getMessage(), e);
                    result.postValue(LoginViewModel.UserResult.ORG_OPERATION_FAILED);
                });
    }

    // joinOrganization method
    // Looks up the org by invite code and writes the matching orgId to the user doc
    // Posts result as a status from UserResult to the LiveData
    public void joinOrganization(String inviteCode, MutableLiveData<LoginViewModel.UserResult> result) {
        String uid = getCurrentUserId();

        // uid or inviteCode null check
        if (uid == null) {
            result.postValue(LoginViewModel.UserResult.ORG_OPERATION_FAILED);
            return;
        }
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            result.postValue(LoginViewModel.UserResult.INVITE_CODE_EMPTY);
            return;
        }

        String normalizedCode = inviteCode.trim().toUpperCase();

        // Query for the organization by invite code
        mFirestore.collection("organizations")
                .whereEqualTo("inviteCode", normalizedCode)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        result.postValue(LoginViewModel.UserResult.INVITE_CODE_INVALID);
                        return;
                    }

                    String orgId = snapshot.getDocuments().get(0).getId();

                    // Write the orgId onto the user doc
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("organizationId", orgId);
                    mFirestore.collection("users").document(uid)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                mCurrentOrgId = orgId;
                                result.postValue(LoginViewModel.UserResult.SUCCESS_ORG_SETUP);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error writing user org: " + e.getMessage(), e);
                                result.postValue(LoginViewModel.UserResult.ORG_OPERATION_FAILED);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error looking up org by invite code: " + e.getMessage(), e);
                    result.postValue(LoginViewModel.UserResult.ORG_OPERATION_FAILED);
                });
    }

    // getCurrentOrgId method
    // Returns the current organizationId for the current user
    // Returns null if not signed in,or if loadCurrentOrgId hasn't completed yet
    public String getCurrentOrgId() {
        return mCurrentOrgId;
    }

    // consumeLastCreatedInviteCode method
    // Returns the invite code from the most recent createOrganization call
    // Returns null if the user joined an existing org or already consumed the code
    public String consumeLastCreatedInviteCode() {
        String code = mLastCreatedInviteCode;
        mLastCreatedInviteCode = null;
        return code;
    }

    // loadCurrentOrgId method
    // Fetches the user's current organization id
    // Used by ItemRepository's auth state listener to wait for the orgId before
    // starting the items listener
    public void loadCurrentOrgId(Runnable onComplete) {
        String uid = getCurrentUserId();

        // uid null check
        if (uid == null) {
            mCurrentOrgId = null;
            if (onComplete != null) onComplete.run();
            return;
        }

        // Fetch the organization id from the users collection
        mFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    mCurrentOrgId = snapshot.exists() ? snapshot.getString("organizationId") : null;
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orgId: " + e.getMessage(), e);
                    mCurrentOrgId = null;
                    if (onComplete != null) onComplete.run();
                });
    }

    // generateInviteCode helper
    // Returns a random 6-char code from an alphabet that excludes visually-ambiguous characters
    private static String generateInviteCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_ALPHABET.charAt(random.nextInt(INVITE_ALPHABET.length())));
        }
        return sb.toString();
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
    // Signs the current user out of the app and clears the cached orgId and invite code
    public void signOut() {
        mAuth.signOut();
        mCurrentOrgId = null;
        mLastCreatedInviteCode = null;
    }
}
