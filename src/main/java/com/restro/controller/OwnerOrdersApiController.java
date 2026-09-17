package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.entity.Order;
import com.restro.entity.Restaurant;
import com.restro.Service.OrderManagementService;
import com.restro.Service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * JSON feed the owner dashboard polls (and gets WebSocket pings for) to
 * refresh its live kitchen board without a full page reload - accepting
 * and serving individual order tickets, order by order. Combined billing
 * across a whole table's session lives in OwnerTableSessionApiController.
 */
@RestController
@RequestMapping("/owner/orders")
public class OwnerOrdersApiController {

    @Autowired
    private OrderManagementService orderManagementService;
    @Autowired
    private RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<?> liveBoard() {
        Restaurant restaurant = restaurantService.getRestaurant();
        List<Order> orders = orderManagementService.liveBoard(restaurant.getRestaurantId());
        return ResponseEntity.ok(Map.of("orders", orders.stream().map(this::toJson).toList()));
    }

    @PostMapping("/{id}/advance")
    public ResponseEntity<?> advance(@PathVariable Integer id) {
        try {
            Order order = orderManagementService.advance(id);
            return ResponseEntity.ok(toJson(order));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Integer id) {
        try {
            Order order = orderManagementService.cancel(id);
            return ResponseEntity.ok(toJson(order));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toJson(Order order) {
        return Map.of(
                "orderId", order.getOrderId(),
                "orderNo", order.getOrderNo(),
                "tableNo", order.getTable().getTableNo(),
                "status", order.getStatus().name(),
                "customerNote", order.getCustomerNote() == null ? "" : order.getCustomerNote(),
                "grandTotal", order.getGrandTotal(),
                "createdAt", order.getCreatedAt().toString(),
                "items", order.getItems().stream().map(i -> Map.of(
                        "name", i.getFoodNameSnapshot(),
                        "quantity", i.getQuantity(),
                        "specialInstructions", i.getSpecialInstructions() == null ? "" : i.getSpecialInstructions()
                )).toList()
        );
    }
}
