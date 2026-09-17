package com.restro.config;

import com.restro.entity.*;
import com.restro.Repo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * First-run bootstrap: creates the single Restaurant row and a default
 * Owner login if none exist yet, so a fresh deployment is usable
 * immediately. Credentials come from app.properties / env vars - change
 * app.owner.default-password before going to production.
 *
 * Also seeds the same demo menu (6 categories, 17 dishes) and the standard
 * CGST+SGST tax split that shipped in the original J2EE project's
 * sql/seed-data.sql, so a fresh install has a real, orderable menu instead
 * of an empty one. Runs once - guarded by "no categories exist yet" - so it
 * never re-inserts or duplicates on subsequent restarts, and never touches
 * anything you've since edited or added yourself. Dish photos were not
 * ported: the original seed only referenced filenames on disk
 * (/uploads/food/*.jpg) that aren't part of this project, and this app
 * stores images as database rows rather than files - upload real photos
 * per item from Menu Management whenever you're ready.
 */
@Component
@Slf4j
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private TaxRepository taxRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @org.springframework.beans.factory.annotation.Value("${app.owner.default-email:owner@restaurant.local}")
    private String defaultEmail;

    @org.springframework.beans.factory.annotation.Value("${app.owner.default-password:ChangeMe123!}")
    private String defaultPassword;

    @org.springframework.beans.factory.annotation.Value("${app.restaurant.default-name:My Restaurant}")
    private String defaultRestaurantName;

    @org.springframework.beans.factory.annotation.Value("${app.seed-demo-menu:true}")
    private boolean seedDemoMenu;

    @Override
    public void run(String... args) {
        Restaurant restaurant = restaurantRepository.findAll().stream().findFirst().orElseGet(() -> {
            log.info("No restaurant row found - seeding a default one");
            return restaurantRepository.save(Restaurant.builder()
                    .name(defaultRestaurantName)
                    .currencyCode("INR")
                    .currencySymbol("\u20B9")
                    .build());
        });

        if (ownerRepository.count() == 0) {
            log.warn("No owner account found - creating default owner login {} / (see app.owner.default-password). " +
                    "CHANGE THIS PASSWORD IMMEDIATELY after first login.", defaultEmail);
            ownerRepository.save(Owner.builder()
                    .restaurant(restaurant)
                    .email(defaultEmail)
                    .passwordHash(passwordEncoder.encode(defaultPassword))
                    .fullName("Owner")
                    .active(true)
                    .build());
        }

        if (seedDemoMenu && categoryRepository.count() == 0) {
            log.info("No menu found - seeding the demo menu (6 categories, 17 dishes) from the original project");
            seedMenu(restaurant);
            seedTaxAndDiscount(restaurant);
        }
    }

    private void seedMenu(Restaurant restaurant) {
        Category starters = category(restaurant, "Starters", 1);
        Category southIndian = category(restaurant, "South Indian", 2);
        Category northIndian = category(restaurant, "North Indian", 3);
        Category chinese = category(restaurant, "Chinese", 4);
        Category beverages = category(restaurant, "Beverages", 5);
        Category desserts = category(restaurant, "Desserts", 6);

        food(restaurant, starters, "Paneer Tikka",
                "Chargrilled cottage cheese marinated in spiced yogurt.",
                "Paneer, yogurt, bell pepper, onion, tikka spices",
                "220.00", "199.00", 20, FoodType.VEG, SpiceLevel.MEDIUM, true, false, 1);
        food(restaurant, starters, "Chicken 65",
                "Deep-fried spicy chicken bites, South Indian style.",
                "Chicken, curry leaves, red chili, yogurt",
                "260.00", null, 20, FoodType.NON_VEG, SpiceLevel.HOT, false, true, 2);
        food(restaurant, starters, "Veg Spring Rolls",
                "Crispy rolls stuffed with mixed vegetables.",
                "Cabbage, carrot, spring roll sheet, soy sauce",
                "180.00", null, 15, FoodType.VEG, SpiceLevel.MILD, false, false, 3);

        food(restaurant, southIndian, "Masala Dosa",
                "Crisp rice crepe filled with spiced potato masala.",
                "Rice batter, potato, mustard seeds, curry leaves",
                "120.00", null, 15, FoodType.VEG, SpiceLevel.MILD, false, true, 1);
        food(restaurant, southIndian, "Idli Sambar",
                "Steamed rice cakes served with lentil sambar.",
                "Rice, urad dal, sambar lentils, vegetables",
                "90.00", null, 10, FoodType.VEG, SpiceLevel.MILD, false, false, 2);
        food(restaurant, southIndian, "Uttapam",
                "Thick savory pancake topped with onion and tomato.",
                "Rice batter, onion, tomato, green chili",
                "130.00", null, 15, FoodType.VEG, SpiceLevel.MEDIUM, false, false, 3);

        food(restaurant, northIndian, "Butter Chicken",
                "Tandoori chicken simmered in creamy tomato gravy.",
                "Chicken, tomato, butter, cream, fenugreek",
                "320.00", null, 25, FoodType.NON_VEG, SpiceLevel.MEDIUM, true, true, 1);
        food(restaurant, northIndian, "Dal Makhani",
                "Slow-cooked black lentils with butter and cream.",
                "Black lentils, kidney beans, butter, cream",
                "210.00", null, 30, FoodType.VEG, SpiceLevel.MILD, false, false, 2);
        food(restaurant, northIndian, "Paneer Butter Masala",
                "Cottage cheese in rich, mildly spiced tomato gravy.",
                "Paneer, tomato, butter, cashew paste",
                "260.00", "240.00", 20, FoodType.VEG, SpiceLevel.MEDIUM, true, false, 3);

        food(restaurant, chinese, "Veg Fried Rice",
                "Wok-tossed rice with fresh vegetables.",
                "Rice, carrot, beans, spring onion, soy sauce",
                "190.00", null, 15, FoodType.VEG, SpiceLevel.MEDIUM, false, false, 1);
        food(restaurant, chinese, "Chicken Manchurian",
                "Indo-Chinese fried chicken in tangy sauce.",
                "Chicken, garlic, soy sauce, spring onion",
                "270.00", null, 20, FoodType.NON_VEG, SpiceLevel.HOT, false, false, 2);
        food(restaurant, chinese, "Chilli Paneer",
                "Cottage cheese tossed in spicy Indo-Chinese sauce.",
                "Paneer, capsicum, soy sauce, red chili",
                "230.00", null, 18, FoodType.VEG, SpiceLevel.HOT, false, false, 3);

        food(restaurant, beverages, "Masala Chai",
                "Spiced Indian milk tea.",
                "Tea leaves, milk, cardamom, ginger",
                "40.00", null, 5, FoodType.VEG, SpiceLevel.MILD, false, false, 1);
        food(restaurant, beverages, "Fresh Lime Soda",
                "Chilled soda with fresh lime, sweet or salted.",
                "Lime, soda water, sugar/salt",
                "60.00", null, 5, FoodType.VEG, SpiceLevel.MILD, false, false, 2);
        food(restaurant, beverages, "Cold Coffee",
                "Blended chilled coffee with milk and ice cream.",
                "Coffee, milk, sugar, ice cream",
                "90.00", null, 8, FoodType.VEG, SpiceLevel.MILD, false, false, 3);

        food(restaurant, desserts, "Gulab Jamun",
                "Soft milk-solid dumplings soaked in rose-cardamom syrup.",
                "Milk solids, sugar syrup, cardamom, rose water",
                "80.00", null, 5, FoodType.VEG, SpiceLevel.MILD, false, true, 1);
        food(restaurant, desserts, "Chocolate Brownie",
                "Warm fudge brownie served with a scoop of vanilla ice cream.",
                "Chocolate, flour, butter, vanilla ice cream",
                "150.00", null, 10, FoodType.VEG, SpiceLevel.MILD, true, false, 2);
    }

    private Category category(Restaurant restaurant, String name, int displayOrder) {
        return categoryRepository.save(Category.builder()
                .restaurant(restaurant)
                .name(name)
                .displayOrder(displayOrder)
                .active(true)
                .build());
    }

    private void food(Restaurant restaurant, Category category, String name, String description,
                       String ingredients, String price, String offerPrice, int prepTimeMinutes,
                       FoodType foodType, SpiceLevel spiceLevel, boolean recommended, boolean bestseller,
                       int displayOrder) {
        foodItemRepository.save(FoodItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(name)
                .description(description)
                .ingredients(ingredients)
                .price(new BigDecimal(price))
                .offerPrice(offerPrice == null ? null : new BigDecimal(offerPrice))
                .prepTimeMinutes(prepTimeMinutes)
                .foodType(foodType)
                .spiceLevel(spiceLevel)
                .available(true)
                .recommended(recommended)
                .bestseller(bestseller)
                .displayOrder(displayOrder)
                .build());
    }

    private void seedTaxAndDiscount(Restaurant restaurant) {
        taxRepository.save(Tax.builder().restaurant(restaurant).name("CGST").percent(new BigDecimal("2.50")).active(true).build());
        taxRepository.save(Tax.builder().restaurant(restaurant).name("SGST").percent(new BigDecimal("2.50")).active(true).build());
        discountRepository.save(Discount.builder()
                .restaurant(restaurant)
                .code("WELCOME10")
                .description("First visit 10% off")
                .discountType(DiscountType.PERCENT)
                .value(new BigDecimal("10.00"))
                .active(true)
                .build());
    }
}
