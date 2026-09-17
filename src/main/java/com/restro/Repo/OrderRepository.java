package com.restro.Repo;

import com.restro.entity.Order;
import com.restro.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    Optional<Order> findByOrderNo(String orderNo);

    /** Live "kitchen + counter" board: everything not yet completed/cancelled, oldest first. */
    @Query("select o from Order o where o.restaurant.restaurantId = :restaurantId " +
           "and o.status not in (com.restro.entity.OrderStatus.COMPLETED, com.restro.entity.OrderStatus.CANCELLED) " +
           "order by o.createdAt asc")
    List<Order> findActiveOrders(@Param("restaurantId") Integer restaurantId);

    /** Orders belonging to one table session, oldest first - used to build the combined bill. */
    List<Order> findByTableSession_TableSessionIdOrderByCreatedAtAsc(Integer tableSessionId);

    List<Order> findByRestaurant_RestaurantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Integer restaurantId, LocalDateTime from, LocalDateTime to);

    List<Order> findByRestaurant_RestaurantIdAndStatusOrderByCreatedAtDesc(Integer restaurantId, OrderStatus status);

    List<Order> findByTable_TableIdOrderByCreatedAtDesc(Integer tableId);

    List<Order> findByRestaurant_RestaurantIdOrderByCreatedAtDesc(Integer restaurantId);

    boolean existsByTable_TableId(Integer tableId);

    boolean existsByDiscount_DiscountId(Integer discountId);
}
