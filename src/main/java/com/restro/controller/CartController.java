package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.dto.Cart;
import com.restro.entity.FoodItem;
import com.restro.Repo.FoodItemRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * AJAX-only endpoint backing the menu page's cart drawer, ported 1:1 from
 * the original CartServlet. Prices are always re-fetched server-side on
 * add - the client never dictates cost.
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @GetMapping
    public Cart getCart(HttpSession session) {
        return getOrCreateCart(session);
    }

    @PostMapping
    public ResponseEntity<?> mutate(HttpSession session, @RequestParam String action,
                                     @RequestParam(required = false) Integer foodItemId,
                                     @RequestParam(required = false, defaultValue = "1") Integer quantity,
                                     @RequestParam(required = false) String note) {
        Integer tableId = (Integer) session.getAttribute(CustomerController.SESSION_TABLE_ID);
        if (tableId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active table session. Please rescan the table QR code."));
        }
        Cart cart = getOrCreateCart(session);
        try {
            switch (action) {
                case "add" -> {
                    FoodItem food = foodItemRepository.findById(foodItemId)
                            .filter(FoodItem::isAvailable)
                            .orElseThrow(() -> new IllegalArgumentException("That item is no longer available."));
                    cart.addOrIncrement(food, Math.max(1, quantity), note);
                }
                case "updateQuantity" -> cart.updateQuantity(foodItemId, quantity);
                case "updateNote" -> cart.updateInstructions(foodItemId, note);
                case "remove" -> cart.removeItem(foodItemId);
                case "clear" -> cart.clear();
                default -> {
                    return ResponseEntity.badRequest().body(Map.of("error", "Unknown cart action: " + action));
                }
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(cart);
    }

    static Cart getOrCreateCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        Integer tableId = (Integer) session.getAttribute(CustomerController.SESSION_TABLE_ID);
        if (cart == null || (tableId != null && !tableId.equals(cart.getTableId()))) {
            cart = new Cart();
            cart.setTableId(tableId);
            session.setAttribute("cart", cart);
        }
        return cart;
    }
}
