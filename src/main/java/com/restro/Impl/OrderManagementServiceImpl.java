package com.restro.Impl;

import com.restro.Repo.OrderRepository;
import com.restro.Service.OrderManagementService;
import com.restro.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderManagementServiceImpl implements OrderManagementService {

    private static final String CHANGED_BY = "OWNER";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Order> liveBoard(Integer restaurantId) {
        return orderRepository.findActiveOrders(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order get(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNo));
    }

    @Override
    @Transactional
    public Order advance(Integer orderId) {
        Order order = get(orderId);
        OrderStatus next = switch (order.getStatus()) {
            case PENDING -> OrderStatus.ACCEPTED;
            case ACCEPTED -> OrderStatus.SERVED;
            default -> throw new IllegalStateException(
                    "Order " + order.getOrderNo() + " cannot be advanced further from " + order.getStatus());
        };
        return setStatus(order, next);
    }

    @Override
    @Transactional
    public Order cancel(Integer orderId) {
        Order order = get(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("A completed/paid order cannot be cancelled.");
        }
        return setStatus(order, OrderStatus.CANCELLED);
    }

    private Order setStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order).status(status).changedBy(CHANGED_BY).build());
        Order saved = orderRepository.save(order);
        eventPublisher.orderChanged(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> ordersBetween(Integer restaurantId, LocalDateTime from, LocalDateTime to) {
        return orderRepository.findByRestaurant_RestaurantIdAndCreatedAtBetweenOrderByCreatedAtDesc(restaurantId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> allOrders(Integer restaurantId) {
        return orderRepository.findByRestaurant_RestaurantIdOrderByCreatedAtDesc(restaurantId);
    }
}
