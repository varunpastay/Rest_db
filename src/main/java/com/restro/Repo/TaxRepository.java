package com.restro.Repo;

import com.restro.entity.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaxRepository extends JpaRepository<Tax, Integer> {
    List<Tax> findByRestaurant_RestaurantIdOrderByNameAsc(Integer restaurantId);
    List<Tax> findByRestaurant_RestaurantIdAndActiveTrue(Integer restaurantId);
}
