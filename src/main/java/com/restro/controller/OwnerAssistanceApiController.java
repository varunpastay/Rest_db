package com.restro.controller;

import com.restro.Service.AssistanceRequestService;
import com.restro.Service.RestaurantService;
import com.restro.entity.AssistanceRequest;
import com.restro.entity.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** JSON feed the owner dashboard polls for open "need help" requests raised from tables. */
@RestController
@RequestMapping("/owner/assistance")
public class OwnerAssistanceApiController {

    @Autowired
    private AssistanceRequestService assistanceRequestService;

    @Autowired
    private RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<?> open() {
        Restaurant restaurant = restaurantService.getRestaurant();
        List<AssistanceRequest> requests = assistanceRequestService.openRequests(restaurant.getRestaurantId());
        return ResponseEntity.ok(Map.of("requests", requests.stream().map(this::toJson).toList()));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable Integer id) {
        try {
            AssistanceRequest request = assistanceRequestService.resolve(id);
            return ResponseEntity.ok(toJson(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toJson(AssistanceRequest request) {
        return Map.of(
                "assistanceRequestId", request.getAssistanceRequestId(),
                "tableNo", request.getTable().getTableNo(),
                "requestType", request.getRequestType().name(),
                "message", request.getMessage() == null ? "" : request.getMessage(),
                "createdAt", request.getCreatedAt().toString()
        );
    }
}
