package com.restro.Repo;

import com.restro.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByRestaurant_RestaurantIdOrderByDisplayOrderAsc(Integer restaurantId);
    List<Category> findByRestaurant_RestaurantIdAndActiveTrueOrderByDisplayOrderAsc(Integer restaurantId);
}
