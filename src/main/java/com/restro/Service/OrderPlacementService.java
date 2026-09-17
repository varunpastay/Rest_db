package com.restro.Service;

import com.restro.dto.Cart;
import com.restro.entity.Order;
import com.restro.entity.Restaurant;

/**
 * Computes an order's money breakdown (subtotal, tax, service charge,
 * discount, grand total) from the cart and the restaurant's current
 * configuration, then persists it.
 */
public interface OrderPlacementService {

    Order placeOrder(Cart cart, Restaurant restaurant, String discountCode, String customerNote);
}
