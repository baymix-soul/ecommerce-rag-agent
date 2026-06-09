package com.ecommerce.rag.services.cart;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.rag.models.dto.CartView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CartServiceTest {

    @Autowired
    private CartService cartService;

    private static final String TEST_USER = "test-cart-user";
    private static final String EXISTING_PRODUCT = "p_beauty_010";
    private static final String NONEXISTENT_PRODUCT = "p_nonexistent_999";

    @Test
    void shouldReturnEmptyCartForNewUser() {
        CartView cart = cartService.getCart(TEST_USER + "-empty");
        
        assertNotNull(cart);
        assertEquals(TEST_USER + "-empty", cart.getUserId());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getTotalQuantity());
        assertEquals(0, cart.getTotalAmount().intValue());
    }

    @Test
    void shouldAddItemToCart() {
        String userId = TEST_USER + "-add";
        cartService.clearCart(userId);
        
        CartView cart = cartService.addItem(userId, EXISTING_PRODUCT, 1);
        
        assertEquals(1, cart.getItems().size());
        assertEquals(1, cart.getTotalQuantity());
        assertNotNull(cart.getItems().get(0).getName());
        assertTrue(cart.getTotalAmount().doubleValue() > 0);
        cartService.clearCart(userId);
    }

    @Test
    void shouldAccumulateQuantityForSameProduct() {
        String userId = TEST_USER + "-accum";
        cartService.clearCart(userId);
        
        cartService.addItem(userId, EXISTING_PRODUCT, 1);
        CartView cart = cartService.addItem(userId, EXISTING_PRODUCT, 2);
        
        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getTotalQuantity());
        assertEquals(3, cart.getItems().get(0).getQuantity());
        cartService.clearCart(userId);
    }

    @Test
    void shouldUpdateItemQuantity() {
        String userId = TEST_USER + "-update";
        cartService.clearCart(userId);
        cartService.addItem(userId, EXISTING_PRODUCT, 1);
        
        CartView cart = cartService.updateItemQuantity(userId, EXISTING_PRODUCT, 5);
        
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(5, cart.getTotalQuantity());
        cartService.clearCart(userId);
    }

    @Test
    void shouldRemoveItemFromCart() {
        String userId = TEST_USER + "-remove";
        cartService.clearCart(userId);
        cartService.addItem(userId, EXISTING_PRODUCT, 1);
        
        CartView cart = cartService.removeItem(userId, EXISTING_PRODUCT);
        
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getTotalQuantity());
    }

    @Test
    void shouldClearCart() {
        String userId = TEST_USER + "-clear";
        cartService.clearCart(userId);
        cartService.addItem(userId, EXISTING_PRODUCT, 1);
        
        CartView cart = cartService.clearCart(userId);
        
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getTotalQuantity());
    }

    @Test
    void shouldThrowExceptionForNonexistentProduct() {
        assertThrows(ProductNotFoundInCartException.class,
                () -> cartService.addItem(TEST_USER + "-nf", NONEXISTENT_PRODUCT, 1));
    }

    @Test
    void shouldThrowExceptionForInvalidQuantityZero() {
        assertThrows(InvalidQuantityException.class,
                () -> cartService.addItem(TEST_USER + "-q", EXISTING_PRODUCT, 0));
    }

    @Test
    void shouldThrowExceptionForInvalidQuantityNegative() {
        assertThrows(InvalidQuantityException.class,
                () -> cartService.addItem(TEST_USER + "-q2", EXISTING_PRODUCT, -1));
    }

    @Test
    void shouldThrowExceptionForInvalidQuantityOver99() {
        assertThrows(InvalidQuantityException.class,
                () -> cartService.addItem(TEST_USER + "-q3", EXISTING_PRODUCT, 100));
    }

    @Test
    void shouldThrowExceptionForUpdateNonexistentItem() {
        assertThrows(CartItemNotFoundException.class,
                () -> cartService.updateItemQuantity(TEST_USER + "-upnf", EXISTING_PRODUCT, 1));
    }

    @Test
    void shouldCalculateCorrectSubtotal() {
        String userId = TEST_USER + "-subtotal";
        cartService.clearCart(userId);
        
        CartView cart = cartService.addItem(userId, EXISTING_PRODUCT, 3);
        
        var item = cart.getItems().get(0);
        double expectedSubtotal = item.getPrice().doubleValue() * 3;
        assertEquals(expectedSubtotal, item.getSubtotal().doubleValue(), 0.01);
        assertEquals(expectedSubtotal, cart.getTotalAmount().doubleValue(), 0.01);
        cartService.clearCart(userId);
    }

    @Test
    void shouldNotRemoveOtherUserItems() {
        String user1 = TEST_USER + "-iso-1";
        String user2 = TEST_USER + "-iso-2";
        cartService.clearCart(user1);
        cartService.clearCart(user2);
        
        cartService.addItem(user1, EXISTING_PRODUCT, 2);
        CartView cart2 = cartService.getCart(user2);
        
        assertTrue(cart2.getItems().isEmpty());
        
        cartService.clearCart(user1);
        cartService.clearCart(user2);
    }
}
