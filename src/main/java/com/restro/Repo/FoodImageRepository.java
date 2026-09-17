package com.restro.Repo;

import com.restro.entity.FoodImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodImageRepository extends JpaRepository<FoodImage, Integer> {
    List<FoodImage> findByFoodItem_FoodItemIdOrderByDisplayOrderAsc(Integer foodItemId);
}
