package com.restro.Impl;

import com.restro.Repo.OrderRepository;
import com.restro.Repo.PaymentRepository;
import com.restro.Repo.RestaurantTableRepository;
import com.restro.Repo.TableSessionRepository;
import com.restro.Service.SequenceService;
import com.restro.Service.TableSessionService;
import com.restro.entity.*;
import com.restro.util.InvoiceNumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TableSessionServiceImpl implements TableSessionService {

    private static final String CHANGED_BY = "OWNER";

    @Autowired
    private TableSessionRepository tableSessionRepository;

    @Autowired
    private RestaurantTableRepository restaurantTableRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Override
    @Transactional
    public TableSession getOrCreateOpenSession(RestaurantTable table) {
        // Row-locks the table for the rest of this transaction so a second, concurrent request
        // for the same table (a second phone ordering at the same instant) can't also see "no
        // open session" and create a duplicate one - see the Javadoc on lockForSessionAssignment.
        restaurantTableRepository.lockForSessionAssignment(table.getTableId());

        return tableSessionRepository.findByTable_TableIdAndStatus(table.getTableId(), TableSessionStatus.OPEN)
                .orElseGet(() -> tableSessionRepository.save(TableSession.builder()
                        .restaurant(table.getRestaurant())
                        .table(table)
                        .status(TableSessionStatus.OPEN)
                        .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> currentSessionOrders(Integer tableId) {
        return tableSessionRepository.findByTable_TableIdAndStatus(tableId, TableSessionStatus.OPEN)
                .map(session -> ordersInSession(session.getTableSessionId()))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> ordersInSession(Integer tableSessionId) {
        return orderRepository.findByTableSession_TableSessionIdOrderByCreatedAtAsc(tableSessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableSession> readyToBillSessions(Integer restaurantId) {
        return tableSessionRepository.findByRestaurant_RestaurantIdAndStatusOrderByOpenedAtAsc(restaurantId, TableSessionStatus.OPEN)
                .stream()
                .filter(session -> {
                    List<Order> orders = ordersInSession(session.getTableSessionId());
                    return !orders.isEmpty() && orders.stream().allMatch(o -> o.getStatus() == OrderStatus.SERVED);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionTotals totals(TableSession session) {
        List<Order> orders = ordersInSession(session.getTableSessionId());
        BigDecimal subtotal = sum(orders, Order::getSubtotal);
        BigDecimal tax = sum(orders, Order::getTaxAmount);
        BigDecimal serviceCharge = sum(orders, Order::getServiceChargeAmount);
        BigDecimal discount = sum(orders, Order::getDiscountAmount);
        BigDecimal grandTotal = sum(orders, Order::getGrandTotal);
        return new SessionTotals(subtotal, tax, serviceCharge, discount, grandTotal);
    }

    private BigDecimal sum(List<Order> orders, java.util.function.Function<Order, BigDecimal> field) {
        return orders.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public Payment settleSession(Integer tableSessionId, PaymentMethod method) {
        TableSession session = get(tableSessionId);
        if (session.getStatus() == TableSessionStatus.CLOSED) {
            throw new IllegalStateException("This table's bill has already been settled.");
        }

        List<Order> orders = ordersInSession(tableSessionId);
        if (orders.isEmpty()) {
            throw new IllegalStateException("This session has no orders to bill.");
        }
        boolean allServed = orders.stream().allMatch(o -> o.getStatus() == OrderStatus.SERVED);
        if (!allServed) {
            throw new IllegalStateException(
                    "Not everything ordered at this table has been served yet - the bill isn't complete.");
        }

        SessionTotals totals = totals(session);
        Payment payment = Payment.builder()
                .tableSession(session)
                .invoiceNo(InvoiceNumberUtil.format(sequenceService.next(InvoiceNumberUtil.todayKey())))
                .amount(totals.grandTotal())
                .method(method)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        for (Order order : orders) {
            order.setStatus(OrderStatus.COMPLETED);
            order.getStatusHistory().add(OrderStatusHistory.builder()
                    .order(order).status(OrderStatus.COMPLETED).changedBy(CHANGED_BY).build());
            Order saved = orderRepository.save(order);
            eventPublisher.orderChanged(saved);
        }

        session.setStatus(TableSessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        tableSessionRepository.save(session);

        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public TableSession get(Integer tableSessionId) {
        return tableSessionRepository.findById(tableSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Table session not found: " + tableSessionId));
    }
}
