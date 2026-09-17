package com.restro.dto;

import com.restro.entity.FoodItem;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/** Session-cart line item. Price is captured server-side when added, never trusted from the client. */
@Getter
@Setter
public class CartItem implements Serializable {

    private Integer foodItemId;
    private String name;
    private BigDecimal unitPrice;
    private int quantity;
    private String specialInstructions;
    private String imagePath;

    public CartItem() {
    }

    public CartItem(FoodItem food, int quantity, String note) {
        this.foodItemId = food.getFoodItemId();
        this.name = food.getName();
        this.unitPrice = food.getEffectivePrice();
        this.quantity = quantity;
        this.specialInstructions = note;
        this.imagePath = food.getPrimaryImagePath();
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
