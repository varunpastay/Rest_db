package com.restro.Service;

import com.restro.entity.*;

import java.util.List;

/**
 * Groups every order placed at a table - regardless of which customer's
 * phone placed it - into one "visit" so the owner bills the table once,
 * not once per phone. A table's session opens automatically with its first
 * order and closes automatically the moment its bill is settled; the next
 * order after that starts a brand new session with nothing carried over.
 */
public interface TableSessionService {

    /**
     * Returns the table's currently open session, creating one if none
     * exists yet. Race-safe across concurrent requests (two phones
     * ordering for the same table at the same instant will never create
     * two separate open sessions) and safe even if this app is ever run
     * as more than one instance, since the safety is a real database lock.
     */
    TableSession getOrCreateOpenSession(RestaurantTable table);

    /** Every order placed so far in a table's currently open session, oldest first - used to show
     *  "already ordered at this table" to any other phone that scans the same table. */
    List<Order> currentSessionOrders(Integer tableId);

    /** Every order belonging to one specific session, oldest first - used both to decide whether
     *  a session is ready to bill and to build the combined item list/totals for its bill. */
    List<Order> ordersInSession(Integer tableSessionId);

    /**
     * Sessions ready to bill: every order in the session has reached
     * SERVED (nothing still pending/preparing), so the combined total
     * won't be missing anything a customer is still waiting on.
     */
    List<TableSession> readyToBillSessions(Integer restaurantId);

    /** Combined grand total (and other combined totals) across every order in a session. */
    SessionTotals totals(TableSession session);

    /**
     * Settles a table's whole session in one action: records a single
     * payment covering every order in it, marks every one of those orders
     * COMPLETED, and closes the session so the table is "free" for the
     * next party - the next order placed at that table starts a fresh
     * session with a running total of zero.
     */
    Payment settleSession(Integer tableSessionId, PaymentMethod method);

    TableSession get(Integer tableSessionId);

    /** Simple aggregate of a session's combined money columns - computed fresh from its orders,
     *  never stored, so it's always consistent with the underlying order rows. */
    record SessionTotals(
            java.math.BigDecimal subtotal,
            java.math.BigDecimal taxAmount,
            java.math.BigDecimal serviceChargeAmount,
            java.math.BigDecimal discountAmount,
            java.math.BigDecimal grandTotal
    ) {
    }
}
