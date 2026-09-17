package com.restro.Repo;

import com.restro.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    List<Discount> findByRestaurant_RestaurantIdOrderByCreatedAtDesc(Integer restaurantId);
    Optional<Discount> findByRestaurant_RestaurantIdAndCodeIgnoreCaseAndActiveTrue(Integer restaurantId, String code);
}
