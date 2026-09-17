package com.restro.dto;

import com.restro.entity.FoodItem;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Kept in the HTTP session (one per browser/table visit) - identical semantics to the old servlet-era cart. */
@Getter
@Setter
public class Cart implements Serializable {

    private Integer tableId;
    private final List<CartItem> items = new ArrayList<>();

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }

    public void addOrIncrement(FoodItem food, int quantity, String note) {
        Optional<CartItem> existing = findItem(food.getFoodItemId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            if (note != null && !note.isBlank()) {
                item.setSpecialInstructions(note);
            }
        } else {
            items.add(new CartItem(food, quantity, note));
        }
    }

    public void updateQuantity(int foodItemId, int quantity) {
        if (quantity <= 0) {
            removeItem(foodItemId);
            return;
        }
        findItem(foodItemId).ifPresent(item -> item.setQuantity(quantity));
    }

    public void updateInstructions(int foodItemId, String note) {
        findItem(foodItemId).ifPresent(item -> item.setSpecialInstructions(note));
    }

    public void removeItem(int foodItemId) {
        items.removeIf(i -> i.getFoodItemId() == foodItemId);
    }

    public BigDecimal getSubtotal() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    private Optional<CartItem> findItem(int foodItemId) {
        return items.stream().filter(i -> i.getFoodItemId() == foodItemId).findFirst();
    }
}
