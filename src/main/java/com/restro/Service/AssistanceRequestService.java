package com.restro.Service;

import com.restro.entity.AssistanceRequest;
import com.restro.entity.AssistanceRequestType;
import com.restro.entity.Restaurant;
import com.restro.entity.RestaurantTable;

import java.util.List;

/**
 * "Need help" requests a customer can raise from their table without placing
 * an order - call the owner over, ask for the table to be cleaned, flag a
 * complaint, or anything else. Shows up live on the owner dashboard right
 * alongside the order board.
 */
public interface AssistanceRequestService {

    AssistanceRequest raise(Restaurant restaurant, RestaurantTable table, AssistanceRequestType type, String message);

    List<AssistanceRequest> openRequests(Integer restaurantId);

    AssistanceRequest resolve(Integer requestId);
}
