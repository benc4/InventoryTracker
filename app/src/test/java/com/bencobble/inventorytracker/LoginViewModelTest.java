package com.bencobble.inventorytracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.bencobble.inventorytracker.repository.UserRepository;
import com.bencobble.inventorytracker.viewmodel.LoginViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// LoginViewModelTest
// Uses Mockito to create a fake UserRepository for testing without Firebase
// Uses InstantTaskExecutorRule to make LiveData updates work synchronously
public class LoginViewModelTest {

    // Runs LiveData updates synchronously
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // Mock UserRepository
    @Mock
    private UserRepository mockUserRepo;

    private LoginViewModel viewModel;

    // setUp
    // Runs before each test to set up mocks and view model
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new LoginViewModel(mockUserRepo);
    }

    /* Login tests */

    @Test
    public void login_emptyEmail_returnsEmptyFields() {
        viewModel.login("", "password123");

        assertEquals(LoginViewModel.UserResult.EMPTY_FIELDS, viewModel.getUserResult().getValue());
        verify(mockUserRepo, never()).login(any(), any(), any());
    }

    @Test
    public void login_emptyPassword_returnsEmptyFields() {
        viewModel.login("test@email.com", "");

        assertEquals(LoginViewModel.UserResult.EMPTY_FIELDS, viewModel.getUserResult().getValue());
        verify(mockUserRepo, never()).login(any(), any(), any());
    }

    @Test
    public void login_bothFieldsEmpty_returnsEmptyFields() {
        viewModel.login("", "");

        assertEquals(LoginViewModel.UserResult.EMPTY_FIELDS, viewModel.getUserResult().getValue());
        verify(mockUserRepo, never()).login(any(), any(), any());
    }

    @Test
    public void login_validCredentials_callsRepository() {
        doAnswer(invocation -> {
            MutableLiveData<LoginViewModel.UserResult> result = invocation.getArgument(2);
            result.setValue(LoginViewModel.UserResult.SUCCESS_LOGIN);
            return null;
        }).when(mockUserRepo).login(eq("test@email.com"), eq("password123"), any());

        viewModel.login("test@email.com", "password123");

        assertEquals(LoginViewModel.UserResult.SUCCESS_LOGIN, viewModel.getUserResult().getValue());
    }

    @Test
    public void login_userNotFound_returnsUserNotFound() {
        doAnswer(invocation -> {
            MutableLiveData<LoginViewModel.UserResult> result = invocation.getArgument(2);
            result.setValue(LoginViewModel.UserResult.USER_NOT_FOUND);
            return null;
        }).when(mockUserRepo).login(eq("unknown@email.com"), eq("password123"), any());

        viewModel.login("unknown@email.com", "password123");

        assertEquals(LoginViewModel.UserResult.USER_NOT_FOUND, viewModel.getUserResult().getValue());
    }

    @Test
    public void login_invalidCredentials_returnsInvalidCredentials() {
        doAnswer(invocation -> {
            MutableLiveData<LoginViewModel.UserResult> result = invocation.getArgument(2);
            result.setValue(LoginViewModel.UserResult.INVALID_CREDENTIALS);
            return null;
        }).when(mockUserRepo).login(eq("test@email.com"), eq("wrongpass"), any());

        viewModel.login("test@email.com", "wrongpass");

        assertEquals(LoginViewModel.UserResult.INVALID_CREDENTIALS, viewModel.getUserResult().getValue());
    }

    /* Create account tests */

    @Test
    public void createAccount_emptyFields_returnsEmptyFields() {
        viewModel.createAccount("", "");

        assertEquals(LoginViewModel.UserResult.EMPTY_FIELDS, viewModel.getUserResult().getValue());
        verify(mockUserRepo, never()).createAccount(any(), any(), any());
    }

    @Test
    public void createAccount_emptyEmail_returnsEmptyFields() {
        viewModel.createAccount("", "pass123");

        assertEquals(LoginViewModel.UserResult.EMPTY_FIELDS, viewModel.getUserResult().getValue());
        verify(mockUserRepo, never()).createAccount(any(), any(), any());
    }

    @Test
    public void createAccount_emptyPassword_returnsEmptyFields() {
        viewModel.createAccount("new@email.com", "");

        assertEquals(LoginViewModel.UserResult.EMPTY_FIELDS, viewModel.getUserResult().getValue());
        verify(mockUserRepo, never()).createAccount(any(), any(), any());
    }

    @Test
    public void createAccount_validCredentials_callsRepository() {
        doAnswer(invocation -> {
            MutableLiveData<LoginViewModel.UserResult> result = invocation.getArgument(2);
            result.setValue(LoginViewModel.UserResult.SUCCESS_CREATE_ACC);
            return null;
        }).when(mockUserRepo).createAccount(eq("new@email.com"), eq("pass123"), any());

        viewModel.createAccount("new@email.com", "pass123");

        assertEquals(LoginViewModel.UserResult.SUCCESS_CREATE_ACC, viewModel.getUserResult().getValue());
    }

    /* isLoggedIn tests */

    @Test
    public void isLoggedIn_whenLoggedIn_returnsTrue() {
        when(mockUserRepo.isLoggedIn()).thenReturn(true);

        assertTrue(viewModel.isLoggedIn());
    }

    @Test
    public void isLoggedIn_whenNotLoggedIn_returnsFalse() {
        when(mockUserRepo.isLoggedIn()).thenReturn(false);

        assertFalse(viewModel.isLoggedIn());
    }
}
