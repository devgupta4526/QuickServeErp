package com.quickserve.modules.order.repository;

import com.quickserve.modules.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByBusinessId(UUID businessId, Pageable pageable);

    Page<Order> findByOutletId(UUID outletId, Pageable pageable);

    Optional<Order> findByIdAndBusinessId(UUID id, UUID businessId);

    List<Order> findByOutletIdAndStatusIn(UUID outletId, List<Order.OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.businessId = :businessId AND o.createdAt >= :from AND o.createdAt < :to")
    long countByBusinessIdAndDateRange(UUID businessId, Instant from, Instant to);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.businessId = :businessId AND o.createdAt >= :from AND o.createdAt < :to AND o.paymentStatus = 'PAID'")
    java.math.BigDecimal sumRevenueByBusinessIdAndDateRange(UUID businessId, Instant from, Instant to);

    @Query("SELECT o.orderNumber FROM Order o WHERE o.outletId = :outletId ORDER BY o.createdAt DESC LIMIT 1")
    Optional<String> findLastOrderNumberByOutletId(UUID outletId);
}
