package com.restro.Repo;

import com.restro.entity.RestaurantTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {
    List<RestaurantTable> findByRestaurant_RestaurantIdOrderByTableNoAsc(Integer restaurantId);
    Optional<RestaurantTable> findByTableIdAndQrToken(Integer tableId, String qrToken);

    /**
     * Row-locks this table for the duration of the caller's transaction (SELECT ... FOR UPDATE
     * under InnoDB). Used only when getting-or-creating a table's open session, so that two
     * phones placing an order for the same table at the exact same moment can't both see "no
     * open session" and each create their own - the second transaction blocks until the first
     * commits, then correctly finds the session the first one just created. This is a real
     * database-level lock, so it stays correct even if this app is ever scaled to more than one
     * instance (unlike an in-process/JVM lock, which would not).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.tableId = :tableId")
    Optional<RestaurantTable> lockForSessionAssignment(@Param("tableId") Integer tableId);
}
