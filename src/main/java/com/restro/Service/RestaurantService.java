package com.restro.Service;

import com.restro.entity.Restaurant;

/** Single-restaurant-per-deployment: there is always exactly one Restaurant row. */
public interface RestaurantService {

    Restaurant getRestaurant();

    Restaurant save(Restaurant restaurant);
}
