package com.restro.Impl;

import com.restro.Repo.RestaurantRepository;
import com.restro.Service.RestaurantService;
import com.restro.entity.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Override
    @Transactional(readOnly = true)
    public Restaurant getRestaurant() {
        return restaurantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No restaurant row found - run the data seed / complete first-time setup"));
    }

    @Override
    @Transactional
    public Restaurant save(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }
}
