package com.restro.Repo;

import com.restro.entity.TableSession;
import com.restro.entity.TableSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, Integer> {

    Optional<TableSession> findByTable_TableIdAndStatus(Integer tableId, TableSessionStatus status);

    List<TableSession> findByRestaurant_RestaurantIdAndStatusOrderByOpenedAtAsc(
            Integer restaurantId, TableSessionStatus status);
}
