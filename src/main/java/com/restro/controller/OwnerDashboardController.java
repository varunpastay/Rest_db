package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.entity.Restaurant;
import com.restro.Service.RestaurantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The unified owner dashboard - replaces the old separate Kitchen dashboard,
 * Counter/Billing dashboard and Admin dashboard with one live orders board:
 * new orders come in, the owner accepts/advances them through prep, marks
 * them served, then bills and marks paid, all on this one screen.
 */
@Controller
public class OwnerDashboardController {

    @Autowired
    private RestaurantService restaurantService;

    @GetMapping("/owner/dashboard")
    public String dashboard(Model model) {
        Restaurant restaurant = restaurantService.getRestaurant();
        model.addAttribute("restaurant", restaurant);
        return "owner/dashboard";
    }
}
