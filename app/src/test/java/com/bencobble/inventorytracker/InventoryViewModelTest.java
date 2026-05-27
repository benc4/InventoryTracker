package com.bencobble.inventorytracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.bencobble.inventorytracker.model.Item;
import com.bencobble.inventorytracker.model.QuantityHistory;
import com.bencobble.inventorytracker.repository.ItemRepository;
import com.bencobble.inventorytracker.viewmodel.InventoryViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// InventoryViewModelTest
// Uses Mockito to create a fake ItemRepository for testing without Firebase
// Uses InstantTaskExecutorRule to make LiveData updates work synchronously
public class InventoryViewModelTest {

    // Runs LiveData updates synchronously
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // Mock ItemRepository
    @Mock
    private ItemRepository mockItemRepo;

    private InventoryViewModel viewModel;
    private MutableLiveData<List<Item>> mockItemsLiveData;

    // Empty observer for MediatorLiveData
    private final Observer<List<Item>> emptyObserver = items -> {};

    // setUp
    // Runs before each test to set up mocks and view model
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockItemsLiveData = new MutableLiveData<>(new ArrayList<>());
        when(mockItemRepo.getItems()).thenReturn(mockItemsLiveData);
        viewModel = new InventoryViewModel(mockItemRepo);
        viewModel.getItems().observeForever(emptyObserver);
    }

    /* addItem tests */

    @Test
    public void addItem_emptyName_returnsEmptyFields() {
        viewModel.addItem("", "description", "5");

        assertEquals(InventoryViewModel.OperationResult.EMPTY_FIELDS,
                viewModel.getOperationResult().getValue());
        verify(mockItemRepo, never()).addItem(any(), any());
    }

    @Test
    public void addItem_emptyQuantity_returnsEmptyFields() {
        viewModel.addItem("Test Item", "description", "");

        assertEquals(InventoryViewModel.OperationResult.EMPTY_FIELDS,
                viewModel.getOperationResult().getValue());
        verify(mockItemRepo, never()).addItem(any(), any());
    }

    @Test
    public void addItem_invalidQuantity_returnsInvalidQuantity() {
        viewModel.addItem("Test Item", "description", "abc");

        assertEquals(InventoryViewModel.OperationResult.INVALID_QUANTITY,
                viewModel.getOperationResult().getValue());
        verify(mockItemRepo, never()).addItem(any(), any());
    }

    @Test
    public void addItem_validInput_callsRepository() {
        doAnswer(invocation -> {
            MutableLiveData<InventoryViewModel.OperationResult> result = invocation.getArgument(1);
            result.setValue(InventoryViewModel.OperationResult.SUCCESS);
            return null;
        }).when(mockItemRepo).addItem(any(Item.class), any());

        viewModel.addItem("Test Item", "A test description", "10");

        assertEquals(InventoryViewModel.OperationResult.SUCCESS,
                viewModel.getOperationResult().getValue());
    }

    /* updateItem tests */

    @Test
    public void updateItem_emptyName_returnsEmptyFields() {
        Item item = new Item("Old Name", "desc", 5);

        viewModel.updateItem(item, "", "description", "5");

        assertEquals(InventoryViewModel.OperationResult.EMPTY_FIELDS,
                viewModel.getOperationResult().getValue());
        verify(mockItemRepo, never()).updateItem(any(), anyInt(), any(), any());
    }

    @Test
    public void updateItem_emptyQuantity_returnsEmptyFields() {
        Item item = new Item("Old Name", "desc", 5);

        viewModel.updateItem(item, "New Name", "description", "");

        assertEquals(InventoryViewModel.OperationResult.EMPTY_FIELDS,
                viewModel.getOperationResult().getValue());
        verify(mockItemRepo, never()).updateItem(any(), anyInt(), any(), any());
    }

    @Test
    public void updateItem_invalidQuantity_returnsInvalidQuantity() {
        Item item = new Item("Old Name", "desc", 5);

        viewModel.updateItem(item, "New Name", "description", "xyz");

        assertEquals(InventoryViewModel.OperationResult.INVALID_QUANTITY,
                viewModel.getOperationResult().getValue());
        verify(mockItemRepo, never()).updateItem(any(), anyInt(), any(), any());
    }

    @Test
    public void updateItem_validInput_callsRepository() {
        Item item = new Item("Old Name", "desc", 5);
        item.setID("test-id");

        doAnswer(invocation -> {
            MutableLiveData<InventoryViewModel.OperationResult> result = invocation.getArgument(2);
            result.setValue(InventoryViewModel.OperationResult.SUCCESS);
            return null;
        }).when(mockItemRepo).updateItem(any(Item.class), eq(5), any(), any());

        viewModel.updateItem(item, "New Name", "new desc", "10");

        assertEquals(InventoryViewModel.OperationResult.SUCCESS,
                viewModel.getOperationResult().getValue());
    }

    @Test
    public void updateItem_quantityToZero_returnsLowStock() {
        Item item = new Item("Test", "desc", 5);
        item.setID("test-id");

        doAnswer(invocation -> {
            MutableLiveData<InventoryViewModel.OperationResult> result = invocation.getArgument(2);
            result.setValue(InventoryViewModel.OperationResult.SUCCESS_LOW_STOCK);
            return null;
        }).when(mockItemRepo).updateItem(any(Item.class), eq(5), any(), any());

        viewModel.updateItem(item, "Test", "desc", "0");

        assertEquals(InventoryViewModel.OperationResult.SUCCESS_LOW_STOCK,
                viewModel.getOperationResult().getValue());
    }

    /* deleteItem tests */

    @Test
    public void deleteItem_callsRepository() {
        Item item = new Item("Test", "desc", 5);
        item.setID("test-id");

        doAnswer(invocation -> {
            MutableLiveData<InventoryViewModel.OperationResult> result = invocation.getArgument(1);
            result.setValue(InventoryViewModel.OperationResult.SUCCESS_DELETE);
            return null;
        }).when(mockItemRepo).deleteItem(eq(item), any());

        viewModel.deleteItem(item);

        assertEquals(InventoryViewModel.OperationResult.SUCCESS_DELETE,
                viewModel.getOperationResult().getValue());
    }

    /* increaseQuantity tests */

    @Test
    public void increaseQuantity_callsRepositoryWithIncreasedQuantity() {
        Item item = new Item("Test", "desc", 5);
        item.setID("test-id");

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);

        viewModel.increaseQuantity(item);

        verify(mockItemRepo).updateItem(itemCaptor.capture(), eq(5), any(), isNull());
        assertEquals(6, itemCaptor.getValue().getQuantity());
        assertEquals("test-id", itemCaptor.getValue().getID());
    }

    /* decreaseQuantity tests */

    @Test
    public void decreaseQuantity_atZero_returnsCannotDecrease() {
        Item item = new Item("Test", "desc", 0);

        viewModel.decreaseQuantity(item);

        assertEquals(InventoryViewModel.OperationResult.CANNOT_DECREASE,
                viewModel.getOperationResult().getValue());
        verify(mockItemRepo, never()).updateItem(any(), anyInt(), any(), any());
    }

    @Test
    public void decreaseQuantity_positive_callsRepositoryWithDecreasedQuantity() {
        Item item = new Item("Test", "desc", 3);
        item.setID("test-id");

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);

        viewModel.decreaseQuantity(item);

        verify(mockItemRepo).updateItem(itemCaptor.capture(), eq(3), any(), any());
        assertEquals(2, itemCaptor.getValue().getQuantity());
        assertEquals("test-id", itemCaptor.getValue().getID());
    }

    @Test
    public void decreaseQuantity_toZero_callsRepository() {
        Item item = new Item("Test", "desc", 1);
        item.setID("test-id");

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);

        viewModel.decreaseQuantity(item);

        verify(mockItemRepo).updateItem(itemCaptor.capture(), eq(1), any(), any());
        assertEquals(0, itemCaptor.getValue().getQuantity());
    }

    /* Search filter tests */

    @Test
    public void searchFilter_returnsMatchingItems() {
        Item item1 = new Item("Apple", "fruit", 5);
        Item item2 = new Item("Banana", "fruit", 3);
        Item item3 = new Item("Avocado", "veggie", 2);

        mockItemsLiveData.setValue(Arrays.asList(item1, item2, item3));

        viewModel.setSearchQuery("ban");

        List<Item> filtered = viewModel.getItems().getValue();
        assertNotNull(filtered);
        assertEquals(1, filtered.size());
        assertEquals("Banana", filtered.get(0).getName());
    }

    @Test
    public void searchFilter_emptyQuery_returnsAllItems() {
        Item item1 = new Item("Apple", "fruit", 5);
        Item item2 = new Item("Banana", "fruit", 3);

        mockItemsLiveData.setValue(Arrays.asList(item1, item2));

        viewModel.setSearchQuery("");

        List<Item> filtered = viewModel.getItems().getValue();
        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void searchFilter_nullQuery_returnsAllItems() {
        Item item1 = new Item("Apple", "fruit", 5);
        Item item2 = new Item("Banana", "fruit", 3);

        mockItemsLiveData.setValue(Arrays.asList(item1, item2));

        viewModel.setSearchQuery(null);

        List<Item> filtered = viewModel.getItems().getValue();
        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void searchFilter_noMatches_returnsEmpty() {
        Item item1 = new Item("Apple", "fruit", 5);
        mockItemsLiveData.setValue(Arrays.asList(item1));

        viewModel.setSearchQuery("xyz");

        List<Item> filtered = viewModel.getItems().getValue();
        assertNotNull(filtered);
        assertEquals(0, filtered.size());
    }

    @Test
    public void searchFilter_caseInsensitive_returnsMatch() {
        Item item1 = new Item("Apple", "fruit", 5);
        mockItemsLiveData.setValue(Arrays.asList(item1));

        viewModel.setSearchQuery("APPLE");

        List<Item> filtered = viewModel.getItems().getValue();
        assertNotNull(filtered);
        assertEquals(1, filtered.size());
        assertEquals("Apple", filtered.get(0).getName());
    }

    /* getItem tests */

    @Test
    public void getItem_delegatesToRepository() {
        MutableLiveData<Item> mockItemData = new MutableLiveData<>();
        when(mockItemRepo.getItem("test-id")).thenReturn(mockItemData);

        LiveData<Item> result = viewModel.getItem("test-id");

        verify(mockItemRepo).getItem("test-id");
        assertEquals(mockItemData, result);
    }

    /* getQuantityHistory tests */

    @Test
    public void getQuantityHistory_delegatesToRepository() {
        MutableLiveData<List<QuantityHistory>> mockHistoryData = new MutableLiveData<>();
        when(mockItemRepo.getQuantityHistory(eq("test-id"), any())).thenReturn(mockHistoryData);

        LiveData<List<QuantityHistory>> result = viewModel.getQuantityHistory("test-id");

        verify(mockItemRepo).getQuantityHistory(eq("test-id"), any());
        assertEquals(mockHistoryData, result);
    }

    /* Listener cleanup tests */

    @Test
    public void stopItemListener_delegatesToRepository() {
        viewModel.stopItemListener();

        verify(mockItemRepo).stopItemListener();
    }

    @Test
    public void stopQuantityHistoryListener_delegatesToRepository() {
        viewModel.stopQuantityHistoryListener();

        verify(mockItemRepo).stopQuantityHistoryListener();
    }

    /* Low stock reset tests */

    @Test
    public void resetLowStockItemName_clearsValue() {
        viewModel.getLowStockItemName().observeForever(name -> {});

        viewModel.resetLowStockItemName();

        assertNull(viewModel.getLowStockItemName().getValue());
    }
}
