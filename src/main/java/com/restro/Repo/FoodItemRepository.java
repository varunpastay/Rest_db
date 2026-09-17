package com.restro.Repo;

import com.restro.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Integer> {
    List<FoodItem> findByRestaurant_RestaurantIdOrderByDisplayOrderAsc(Integer restaurantId);
    List<FoodItem> findByRestaurant_RestaurantIdAndAvailableTrueOrderByDisplayOrderAsc(Integer restaurantId);
    List<FoodItem> findByCategory_CategoryIdOrderByDisplayOrderAsc(Integer categoryId);
}
