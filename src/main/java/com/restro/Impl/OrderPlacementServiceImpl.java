package com.restro.Impl;

import com.restro.Repo.DiscountRepository;
import com.restro.Repo.OrderRepository;
import com.restro.Repo.RestaurantTableRepository;
import com.restro.Repo.TaxRepository;
import com.restro.Service.OrderPlacementService;
import com.restro.Service.SequenceService;
import com.restro.Service.TableSessionService;
import com.restro.dto.Cart;
import com.restro.dto.CartItem;
import com.restro.entity.*;
import com.restro.util.OrderNumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderPlacementServiceImpl implements OrderPlacementService {

    private static final int MONEY_SCALE = 2;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TaxRepository taxRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Autowired
    private TableSessionService tableSessionService;

    @Autowired
    private SequenceService sequenceService;

    @Override
    @Transactional
    public Order placeOrder(Cart cart, Restaurant restaurant, String discountCode, String customerNote) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty cart");
        }
        if (!restaurant.isOpen()) {
            throw new IllegalStateException("The restaurant is currently closed and not accepting orders.");
        }

        RestaurantTable table = tableRepository.findById(cart.getTableId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid table"));

        BigDecimal subtotal = round(cart.getSubtotal());

        BigDecimal taxPercent = taxRepository.findByRestaurant_RestaurantIdAndActiveTrue(restaurant.getRestaurantId())
                .stream().map(Tax::getPercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = round(subtotal.multiply(taxPercent).divide(BigDecimal.valueOf(100)));

        BigDecimal serviceChargePercent = restaurant.getServiceChargePercent() != null
                ? restaurant.getServiceChargePercent() : BigDecimal.ZERO;
        BigDecimal serviceChargeAmount = round(subtotal.multiply(serviceChargePercent).divide(BigDecimal.valueOf(100)));

        Discount discount = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountCode != null && !discountCode.isBlank()) {
            discount = discountRepository
                    .findByRestaurant_RestaurantIdAndCodeIgnoreCaseAndActiveTrue(restaurant.getRestaurantId(), discountCode)
                    .orElse(null);
            if (discount != null) {
                discountAmount = discount.getDiscountType() == DiscountType.PERCENT
                        ? round(subtotal.multiply(discount.getValue()).divide(BigDecimal.valueOf(100)))
                        : round(discount.getValue());
                if (discountAmount.compareTo(subtotal) > 0) {
                    discountAmount = subtotal;
                }
            }
        }

        BigDecimal grandTotal = subtotal.add(taxAmount).add(serviceChargeAmount).subtract(discountAmount);

        // Attach to the table's currently open session (creating one if this is the first order
        // since the table was last free) so every phone ordering at this table bills together.
        TableSession session = tableSessionService.getOrCreateOpenSession(table);

        Order order = Order.builder()
                .restaurant(restaurant)
                .orderNo(OrderNumberUtil.format(sequenceService.next(OrderNumberUtil.todayKey())))
                .table(table)
                .tableSession(session)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .serviceChargeAmount(serviceChargeAmount)
                .discountAmount(discountAmount)
                .grandTotal(grandTotal)
                .discount(discount)
                .customerNote(customerNote)
                .build();

        List<OrderItem> items = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            items.add(OrderItem.builder()
                    .order(order)
                    .foodItem(FoodItem.builder().foodItemId(cartItem.getFoodItemId()).build())
                    .foodNameSnapshot(cartItem.getName())
                    .unitPrice(cartItem.getUnitPrice())
                    .quantity(cartItem.getQuantity())
                    .specialInstructions(cartItem.getSpecialInstructions())
                    .lineTotal(round(cartItem.getLineTotal()))
                    .build());
        }
        order.setItems(items);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order).status(OrderStatus.PENDING).changedBy("CUSTOMER").build());

        Order saved = orderRepository.save(order);
        eventPublisher.orderChanged(saved);
        return saved;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
