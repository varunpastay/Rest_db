package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.dto.Cart;
import com.restro.entity.Order;
import com.restro.entity.Restaurant;
import com.restro.entity.RestaurantTable;
import com.restro.Service.MenuService;
import com.restro.Service.RestaurantService;
import com.restro.Service.TableService;
import com.restro.Service.TableSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * No-login customer flow: scan QR -> /t/{tableId}/{token} -> menu opens with
 * the table bound to the session, same as the original app's
 * ?table=&token= entry point.
 */
@Controller
public class CustomerController {

    public static final String SESSION_TABLE_ID = "tableId";
    public static final String SESSION_TABLE_NO = "tableNo";

    @Autowired
    private TableService tableService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private TableSessionService tableSessionService;

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("restaurant", restaurantService.getRestaurant());
        return "customer/landing";
    }

    @GetMapping("/t/{tableId}/{token}")
    public String enterTable(@PathVariable Integer tableId, @PathVariable String token,
                              HttpSession session, Model model) {
        Optional<RestaurantTable> table = tableService.validate(tableId, token);
        if (table.isEmpty()) {
            return "customer/invalid-table";
        }
        session.setAttribute(SESSION_TABLE_ID, table.get().getTableId());
        session.setAttribute(SESSION_TABLE_NO, table.get().getTableNo());
        session.removeAttribute("cart");
        return "redirect:/menu";
    }

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model) {
        Integer tableId = (Integer) session.getAttribute(SESSION_TABLE_ID);
        if (tableId == null) {
            return "customer/invalid-table";
        }
        Restaurant restaurant = restaurantService.getRestaurant();
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("categories", menuService.activeCategories(restaurant.getRestaurantId()));
        model.addAttribute("foodItems", menuService.availableFoodItems(restaurant.getRestaurantId()));
        model.addAttribute("tableNo", session.getAttribute(SESSION_TABLE_NO));
        model.addAttribute("tableId", tableId);
        return "customer/menu";
    }

    /**
     * Shared "already ordered at this table" feed - every phone that scans this table's QR sees
     * what everyone else at the table has ordered so far this visit, not just their own cart.
     * Polled from the menu page so it updates within a few seconds of someone else ordering.
     */
    @GetMapping("/menu/table-orders")
    @ResponseBody
    public ResponseEntity<?> tableOrders(HttpSession session) {
        Integer tableId = (Integer) session.getAttribute(SESSION_TABLE_ID);
        if (tableId == null) {
            return ResponseEntity.ok(Map.of("items", List.of()));
        }
        List<Order> orders = tableSessionService.currentSessionOrders(tableId);
        List<Map<String, Object>> items = orders.stream()
                .flatMap(o -> o.getItems().stream().map(i -> Map.<String, Object>of(
                        "name", i.getFoodNameSnapshot(),
                        "quantity", i.getQuantity(),
                        "orderStatus", o.getStatus().name()
                )))
                .toList();
        return ResponseEntity.ok(Map.of("items", items));
    }
}
