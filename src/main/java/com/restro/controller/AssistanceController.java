package com.restro.controller;

import com.restro.Repo.RestaurantTableRepository;
import com.restro.Service.AssistanceRequestService;
import com.restro.Service.RestaurantService;
import com.restro.entity.AssistanceRequestType;
import com.restro.entity.RestaurantTable;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Customer-facing "need help" button - no login, tied to whatever table the QR scan put in the session. */
@RestController
public class AssistanceController {

    @Autowired
    private AssistanceRequestService assistanceRequestService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @PostMapping("/assistance/request")
    public ResponseEntity<?> raise(HttpSession session,
                                    @RequestParam AssistanceRequestType type,
                                    @RequestParam(required = false) String message) {
        Integer tableId = (Integer) session.getAttribute(CustomerController.SESSION_TABLE_ID);
        if (tableId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active table session. Please rescan the table QR code."));
        }
        RestaurantTable table = tableRepository.findById(tableId)
                .orElse(null);
        if (table == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Table not found."));
        }
        assistanceRequestService.raise(restaurantService.getRestaurant(), table, type, message);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
