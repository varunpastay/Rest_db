package com.restro.controller;

import com.restro.Service.RestaurantService;
import com.restro.Service.TableSessionService;
import com.restro.entity.Order;
import com.restro.entity.OrderItem;
import com.restro.entity.PaymentMethod;
import com.restro.entity.Restaurant;
import com.restro.entity.TableSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Combined table billing: one card per table (not per order), covering
 * every order any phone at that table placed since it was last free.
 * Only appears once every order in the session has been Served, so the
 * bill is never missing something still cooking.
 */
@RestController
@RequestMapping("/owner/table-sessions")
public class OwnerTableSessionApiController {

    @Autowired
    private TableSessionService tableSessionService;

    @Autowired
    private RestaurantService restaurantService;

    @GetMapping("/billing")
    public ResponseEntity<?> readyToBill() {
        Restaurant restaurant = restaurantService.getRestaurant();
        List<TableSession> sessions = tableSessionService.readyToBillSessions(restaurant.getRestaurantId());
        return ResponseEntity.ok(Map.of("sessions", sessions.stream().map(this::toJson).toList()));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<?> settle(@PathVariable Integer id, @RequestParam(defaultValue = "CASH") PaymentMethod method) {
        try {
            tableSessionService.settleSession(id, method);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toJson(TableSession session) {
        List<Order> orders = tableSessionService.ordersInSession(session.getTableSessionId());
        var totals = tableSessionService.totals(session);

        List<Map<String, Object>> items = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(this::itemJson)
                .toList();

        return Map.ofEntries(
                Map.entry("tableSessionId", session.getTableSessionId()),
                Map.entry("tableNo", session.getTable().getTableNo()),
                Map.entry("orderCount", orders.size()),
                Map.entry("orderNos", orders.stream().map(Order::getOrderNo).toList()),
                Map.entry("items", items),
                Map.entry("subtotal", totals.subtotal()),
                Map.entry("taxAmount", totals.taxAmount()),
                Map.entry("serviceChargeAmount", totals.serviceChargeAmount()),
                Map.entry("discountAmount", totals.discountAmount()),
                Map.entry("grandTotal", totals.grandTotal()),
                Map.entry("openedAt", session.getOpenedAt().toString())
        );
    }

    private Map<String, Object> itemJson(OrderItem item) {
        return Map.of(
                "name", item.getFoodNameSnapshot(),
                "quantity", item.getQuantity(),
                "specialInstructions", item.getSpecialInstructions() == null ? "" : item.getSpecialInstructions()
        );
    }
}
