package com.restro.Impl;

import com.restro.Repo.OrderRepository;
import com.restro.Service.ReportService;
import com.restro.entity.Order;
import com.restro.entity.OrderItem;
import com.restro.entity.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM");

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportResult generate(Integer restaurantId, Range range) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = switch (range) {
            case TODAY -> now.toLocalDate().atStartOfDay();
            case WEEK -> now.minusDays(7);
            case MONTH -> now.minusDays(30);
            case YEAR -> now.minusDays(365);
        };

        List<Order> orders = orderRepository
                .findByRestaurant_RestaurantIdAndCreatedAtBetweenOrderByCreatedAtDesc(restaurantId, from, now)
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .toList();

        ReportResult result = new ReportResult();
        result.orders = orders;
        result.totalRevenue = orders.stream().map(Order::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.orderCount = orders.size();
        result.averageOrderValue = orders.isEmpty() ? BigDecimal.ZERO
                : result.totalRevenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);

        Map<String, Integer> soldCount = new HashMap<>();
        Map<Integer, BigDecimal> hourRevenue = new TreeMap<>();
        Map<LocalDate, BigDecimal> dayRevenue = new TreeMap<>();
        Map<LocalDate, Integer> dayOrders = new TreeMap<>();
        for (Order o : orders) {
            for (OrderItem item : o.getItems()) {
                soldCount.merge(item.getFoodNameSnapshot(), item.getQuantity(), Integer::sum);
            }
            int hour = o.getCreatedAt().getHour();
            hourRevenue.merge(hour, o.getGrandTotal(), BigDecimal::add);

            LocalDate day = o.getCreatedAt().toLocalDate();
            dayRevenue.merge(day, o.getGrandTotal(), BigDecimal::add);
            dayOrders.merge(day, 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> sortedByQty = soldCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).collect(Collectors.toList());
        List<Map.Entry<String, Integer>> topSellers = sortedByQty.stream().limit(10).toList();
        result.leastSellers = sortedByQty.stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue)).limit(10).toList();

        int maxTopSellerCount = topSellers.stream().mapToInt(Map.Entry::getValue).max().orElse(0);
        result.topSellerBars = topSellers.stream()
                .map(e -> new BarItem(e.getKey(), e.getValue() + " sold", percentOf(e.getValue(), maxTopSellerCount)))
                .toList();

        BigDecimal maxHourRevenue = hourRevenue.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        result.peakHourBars = hourRevenue.entrySet().stream()
                .map(e -> new BarItem(
                        String.format("%02d:00 - %02d:00", e.getKey(), (e.getKey() + 1) % 24),
                        currencyDisplay(e.getValue()),
                        percentOf(e.getValue(), maxHourRevenue)))
                .toList();

        BigDecimal maxDayRevenue = dayRevenue.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        result.revenueTrendBars = dayRevenue.entrySet().stream()
                .map(e -> new BarItem(
                        e.getKey().format(DAY_LABEL),
                        currencyDisplay(e.getValue()) + " (" + dayOrders.getOrDefault(e.getKey(), 0) + ")",
                        percentOf(e.getValue(), maxDayRevenue)))
                .toList();

        result.trendDayLabels = dayRevenue.keySet().stream().map(d -> d.format(DAY_LABEL)).toList();
        result.trendDayRevenue = new ArrayList<>(dayRevenue.values());

        return result;
    }

    private String currencyDisplay(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private int percentOf(int value, int max) {
        if (max <= 0) return 0;
        return Math.max(4, Math.min(100, Math.round(value * 100f / max)));
    }

    private int percentOf(BigDecimal value, BigDecimal max) {
        if (max == null || max.signum() <= 0) return 0;
        int pct = value.multiply(BigDecimal.valueOf(100))
                .divide(max, 0, RoundingMode.HALF_UP).intValue();
        return Math.max(4, Math.min(100, pct));
    }
}
