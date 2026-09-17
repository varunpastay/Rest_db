package com.restro.Service;

import com.restro.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Sales Reports: Today/Week/Month/Year revenue, top/least sellers, peak hours (by revenue),
 *  day-by-day revenue trend, average order value. */
public interface ReportService {

    enum Range { TODAY, WEEK, MONTH, YEAR }

    /** One row in a bar-chart panel - percent (0-100) is pre-computed server-side against that
     *  panel's max value, so templates just render a div with width: X% and never do BigDecimal
     *  arithmetic themselves. */
    @Getter
    @AllArgsConstructor
    class BarItem {
        private final String label;
        private final String valueDisplay;
        private final int percent;
    }

    @Getter
    class ReportResult {
        public BigDecimal totalRevenue = BigDecimal.ZERO;
        public int orderCount;
        public BigDecimal averageOrderValue = BigDecimal.ZERO;

        /** Top-selling dishes by quantity, with bar width relative to the best seller. */
        public List<BarItem> topSellerBars = List.of();
        /** Slowest-selling dishes by quantity - plain list, no bar (see reports.html for why). */
        public List<Map.Entry<String, Integer>> leastSellers = List.of();
        /** Revenue per hour-of-day that had at least one order, chronological, bar relative to the busiest hour. */
        public List<BarItem> peakHourBars = List.of();
        /** Revenue per calendar day in the selected range, chronological, bar relative to the best day; label carries the order count too. */
        public List<BarItem> revenueTrendBars = List.of();

        /** Raw (label -> revenue) pairs for the Chart.js line chart, chronological. */
        public List<String> trendDayLabels = List.of();
        public List<BigDecimal> trendDayRevenue = List.of();

        public List<Order> orders = List.of();
    }

    ReportResult generate(Integer restaurantId, Range range);
}
