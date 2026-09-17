package com.restro.Repo;

import com.restro.entity.AssistanceRequest;
import com.restro.entity.AssistanceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssistanceRequestRepository extends JpaRepository<AssistanceRequest, Integer> {

    List<AssistanceRequest> findByRestaurant_RestaurantIdAndStatusOrderByCreatedAtAsc(
            Integer restaurantId, AssistanceRequestStatus status);

    List<AssistanceRequest> findByTable_TableIdOrderByCreatedAtDesc(Integer tableId);

    void deleteByTable_TableId(Integer tableId);
}
