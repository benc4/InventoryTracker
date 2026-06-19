package com.bencobble.inventorytracker.fragments;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bencobble.inventorytracker.R;
import com.bencobble.inventorytracker.viewmodel.LoginViewModel;

import dagger.hilt.android.AndroidEntryPoint;

// OrgSetupFragment
// Shown after account creation, or on login if the user has no organization yet
// Lets the user create a new organization or join an existing one with an invite code
// Dependency injection managed by Hilt
@AndroidEntryPoint
public class OrgSetupFragment extends Fragment {
    /* View references */
    private EditText mEditTextOrgName;
    private EditText mEditTextInviteCode;
    private Button mButtonCreateOrg;
    private Button mButtonJoinOrg;
    private TextView mTextViewStatus;
    private ContentLoadingProgressBar mProgressSpinner;

    private LoginViewModel mLoginViewModel;

    // onCreateView override method
    // Inflates the layout XML
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_setup, container, false);
    }

    // onViewCreated override method
    // Handles setup after the view is created
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEditTextOrgName = view.findViewById(R.id.editTextOrgName);
        mEditTextInviteCode = view.findViewById(R.id.editTextInviteCode);
        mButtonCreateOrg = view.findViewById(R.id.buttonCreateOrg);
        mButtonJoinOrg = view.findViewById(R.id.buttonJoinOrg);
        mTextViewStatus = view.findViewById(R.id.textViewOrgStatus);
        mProgressSpinner = view.findViewById(R.id.orgProgressSpinner);

        // Gets LoginViewModel
        // UserRepository is injected into it by Hilt
        // Reuses the same ViewModel/LiveData as LoginFragment so the org methods
        // post results to the same UserResult stream the auth flow already uses
        mLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // getUserResult observer
        // Observes changes to the UserResult LiveData
        // Calls processResult to handle the UserResult when it updates
        mLoginViewModel.getUserResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) { // Skip if result is null (fragment first created)
                processResult(result);
            }
        });

        // Create button listener
        // Retrieves the org name from EditText and calls createOrganization
        mButtonCreateOrg.setOnClickListener(v -> {
            String name = mEditTextOrgName.getText().toString().trim();
            disableOrgUI();
            mLoginViewModel.createOrganization(name);
        });

        // Join button listener
        // Retrieves the invite code from EditText and calls joinOrganization
        mButtonJoinOrg.setOnClickListener(v -> {
            String code = mEditTextInviteCode.getText().toString().trim();
            disableOrgUI();
            mLoginViewModel.joinOrganization(code);
        });
    }

    // processResult method
    // Takes a UserResult code as a parameter
    // On success, shows the invite code dialog then navigates to the grid,
    // otherwise shows the status message
    private void processResult(LoginViewModel.UserResult result) {
        if (result == LoginViewModel.UserResult.SUCCESS_ORG_SETUP) {
            String inviteCode = mLoginViewModel.consumeLastCreatedInviteCode();
            if (inviteCode != null) {
                showInviteCodeDialog(inviteCode);
            } else {
                navigateToGrid();
            }
            return;
        }

        showStatus(result);
        enableOrgUI();
    }

    // showInviteCodeDialog method
    // Displays the invite code in an AlertDialog
    // Navigates to the grid when the user dismisses the dialog
    private void showInviteCodeDialog(String inviteCode) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.invite_code_dialog_title)
                .setMessage(getString(R.string.invite_code_dialog_message, inviteCode))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> navigateToGrid())
                .create()
                .show();
    }

    // navigateToGrid method
    // Navigates to InventoryGridFragment after org setup completes
    private void navigateToGrid() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_orgSetup_to_grid);
    }

    // showStatus method
    // Takes a UserResult result code as a parameter
    // Uses a switch to set the status text based on the result code
    private void showStatus(LoginViewModel.UserResult result) {
        String message;

        switch (result) {
            case ORG_NAME_EMPTY:
                message = getString(R.string.org_name_empty);
                break;
            case INVITE_CODE_EMPTY:
                message = getString(R.string.invite_code_empty);
                break;
            case INVITE_CODE_INVALID:
                message = getString(R.string.invite_code_invalid);
                break;
            case ORG_OPERATION_FAILED:
            default:
                message = getString(R.string.org_operation_failed);
                break;
        }

        // Set status text and color
        mTextViewStatus.setText(message);
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.textColorError, typedValue, true);
        mTextViewStatus.setTextColor(typedValue.data);
        mTextViewStatus.setVisibility(TextView.VISIBLE);
    }

    /* UI state helpers */

    // disableOrgUI method
    // Disables inputs during a create/join operation
    private void disableOrgUI() {
        mEditTextOrgName.setEnabled(false);
        mEditTextInviteCode.setEnabled(false);
        mButtonCreateOrg.setEnabled(false);
        mButtonJoinOrg.setEnabled(false);

        mTextViewStatus.setText("");
        mTextViewStatus.setVisibility(TextView.GONE);
        mProgressSpinner.show();
    }

    // enableOrgUI method
    // Re-enables inputs after a failed create/join so the user can retry
    private void enableOrgUI() {
        mEditTextOrgName.setEnabled(true);
        mEditTextInviteCode.setEnabled(true);
        mButtonCreateOrg.setEnabled(true);
        mButtonJoinOrg.setEnabled(true);

        mProgressSpinner.hide();
    }
}
