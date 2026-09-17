package com.restro.Service;

import com.restro.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the old KitchenDashboard servlet did - accept/advance an
 * order through its kitchen lifecycle, order by order. Billing (combining
 * every order at a table into one payment) lives in TableSessionService
 * instead, since that now operates on a whole table's session rather than
 * a single order.
 */
public interface OrderManagementService {

    List<Order> liveBoard(Integer restaurantId);

    Order get(Integer orderId);

    Order getByOrderNo(String orderNo);

    /** One-click "advance to the next step" - Pending -> Accepted -> Served. */
    Order advance(Integer orderId);

    Order cancel(Integer orderId);

    List<Order> ordersBetween(Integer restaurantId, LocalDateTime from, LocalDateTime to);

    List<Order> allOrders(Integer restaurantId);
}
