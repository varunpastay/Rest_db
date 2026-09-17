package com.restro.controller;

import com.restro.Service.OrderManagementService;
import com.restro.Service.RestaurantService;
import com.restro.entity.Order;
import com.restro.entity.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Lets the owner browse every order ever placed - including ones that have
 * already been paid/completed and dropped off the live "Live Orders" and
 * "Ready to Bill" boards - and see exactly what a customer ordered.
 */
@Controller
public class OwnerOrderHistoryController {

    @Autowired
    private OrderManagementService orderManagementService;

    @Autowired
    private RestaurantService restaurantService;

    @GetMapping("/owner/orders/history")
    public String history(Model model) {
        Restaurant restaurant = restaurantService.getRestaurant();
        List<Order> orders = orderManagementService.allOrders(restaurant.getRestaurantId());
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("orders", orders);
        return "owner/order-history";
    }
}
