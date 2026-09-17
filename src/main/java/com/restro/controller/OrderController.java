package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.dto.Cart;
import com.restro.entity.Order;
import com.restro.entity.Restaurant;
import com.restro.Service.OrderManagementService;
import com.restro.Service.OrderPlacementService;
import com.restro.Service.RestaurantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Customer order placement + live status tracking (AJAX polling, same pattern as the original order-track.js). */
@Controller
public class OrderController {

    @Autowired
    private OrderPlacementService orderPlacementService;
    @Autowired
    private OrderManagementService orderManagementService;
    @Autowired
    private RestaurantService restaurantService;

    @PostMapping("/order/place")
    @ResponseBody
    public ResponseEntity<?> place(HttpSession session,
                                    @RequestParam(required = false) String discountCode,
                                    @RequestParam(required = false) String customerNote) {
        Cart cart = CartController.getOrCreateCart(session);
        try {
            Restaurant restaurant = restaurantService.getRestaurant();
            Order order = orderPlacementService.placeOrder(cart, restaurant, discountCode, customerNote);
            cart.clear();
            return ResponseEntity.ok(Map.of("orderNo", order.getOrderNo(), "orderId", order.getOrderId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/order/track/{orderNo}")
    public String track(@PathVariable String orderNo, Model model) {
        model.addAttribute("orderNo", orderNo);
        model.addAttribute("restaurant", restaurantService.getRestaurant());
        return "customer/order-track";
    }

    @GetMapping("/order/status/{orderNo}")
    @ResponseBody
    public ResponseEntity<?> status(@PathVariable String orderNo) {
        try {
            Order order = orderManagementService.getByOrderNo(orderNo);
            return ResponseEntity.ok(Map.of(
                    "orderNo", order.getOrderNo(),
                    "status", order.getStatus().name(),
                    "tableNo", order.getTable().getTableNo(),
                    "grandTotal", order.getGrandTotal(),
                    "items", order.getItems().stream().map(i -> Map.of(
                            "name", i.getFoodNameSnapshot(),
                            "quantity", i.getQuantity(),
                            "lineTotal", i.getLineTotal()
                    )).toList()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Order not found."));
        }
    }
}
