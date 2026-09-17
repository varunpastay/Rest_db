package com.restro.Repo;

import com.restro.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Integer> {
    List<Settings> findByRestaurant_RestaurantId(Integer restaurantId);
    Optional<Settings> findByRestaurant_RestaurantIdAndSettingKey(Integer restaurantId, String key);
}
