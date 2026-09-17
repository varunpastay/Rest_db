-- =============================================================================
-- Demo menu seed for rest_db - run this directly in MySQL (Workbench, CLI, etc).
-- Matches exactly what the app's own DataSeeder would insert on first run,
-- so this is safe to use whether or not the app has already auto-seeded.
--
-- IMPORTANT: run this AFTER the app has started at least once (so the
-- restaurant/owner rows already exist - Hibernate needs to have created the
-- tables via ddl-auto=update first).
--
-- Safe to re-run: the guard at the very top skips everything if a "Starters"
-- category already exists for this restaurant, so you won't get duplicates.
-- =============================================================================

USE rest_db;

-- Only proceed if the demo menu hasn't already been seeded (by this script or the app itself)
SET @already_seeded = (
    SELECT COUNT(*) FROM category
    WHERE restaurant_id = (SELECT restaurant_id FROM restaurant LIMIT 1)
      AND name = 'Starters'
);

-- Grab the one restaurant row's id (this app is single-restaurant-per-deployment)
SET @restaurant_id = (SELECT restaurant_id FROM restaurant LIMIT 1);

-- -----------------------------------------------------------------------------
-- category (6 rows)
-- -----------------------------------------------------------------------------
INSERT INTO category (restaurant_id, name, display_order, is_active, created_at, updated_at)
SELECT @restaurant_id, 'Starters', 1, 1, NOW(), NOW() WHERE @already_seeded = 0;
INSERT INTO category (restaurant_id, name, display_order, is_active, created_at, updated_at)
SELECT @restaurant_id, 'South Indian', 2, 1, NOW(), NOW() WHERE @already_seeded = 0;
INSERT INTO category (restaurant_id, name, display_order, is_active, created_at, updated_at)
SELECT @restaurant_id, 'North Indian', 3, 1, NOW(), NOW() WHERE @already_seeded = 0;
INSERT INTO category (restaurant_id, name, display_order, is_active, created_at, updated_at)
SELECT @restaurant_id, 'Chinese', 4, 1, NOW(), NOW() WHERE @already_seeded = 0;
INSERT INTO category (restaurant_id, name, display_order, is_active, created_at, updated_at)
SELECT @restaurant_id, 'Beverages', 5, 1, NOW(), NOW() WHERE @already_seeded = 0;
INSERT INTO category (restaurant_id, name, display_order, is_active, created_at, updated_at)
SELECT @restaurant_id, 'Desserts', 6, 1, NOW(), NOW() WHERE @already_seeded = 0;

-- Look up the category ids we just created (or that already existed)
SET @cat_starters     = (SELECT category_id FROM category WHERE restaurant_id = @restaurant_id AND name = 'Starters');
SET @cat_south_indian  = (SELECT category_id FROM category WHERE restaurant_id = @restaurant_id AND name = 'South Indian');
SET @cat_north_indian  = (SELECT category_id FROM category WHERE restaurant_id = @restaurant_id AND name = 'North Indian');
SET @cat_chinese       = (SELECT category_id FROM category WHERE restaurant_id = @restaurant_id AND name = 'Chinese');
SET @cat_beverages     = (SELECT category_id FROM category WHERE restaurant_id = @restaurant_id AND name = 'Beverages');
SET @cat_desserts      = (SELECT category_id FROM category WHERE restaurant_id = @restaurant_id AND name = 'Desserts');

-- -----------------------------------------------------------------------------
-- food_item (17 rows)
-- -----------------------------------------------------------------------------
INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_starters, 'Paneer Tikka',
    'Chargrilled cottage cheese marinated in spiced yogurt.',
    'Paneer, yogurt, bell pepper, onion, tikka spices',
    220.00, 199.00, 20, 'VEG', 'MEDIUM', 1, 1, 0, 1, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_starters, 'Chicken 65',
    'Deep-fried spicy chicken bites, South Indian style.',
    'Chicken, curry leaves, red chili, yogurt',
    260.00, NULL, 20, 'NON_VEG', 'HOT', 1, 0, 1, 2, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_starters, 'Veg Spring Rolls',
    'Crispy rolls stuffed with mixed vegetables.',
    'Cabbage, carrot, spring roll sheet, soy sauce',
    180.00, NULL, 15, 'VEG', 'MILD', 1, 0, 0, 3, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_south_indian, 'Masala Dosa',
    'Crisp rice crepe filled with spiced potato masala.',
    'Rice batter, potato, mustard seeds, curry leaves',
    120.00, NULL, 15, 'VEG', 'MILD', 1, 0, 1, 1, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_south_indian, 'Idli Sambar',
    'Steamed rice cakes served with lentil sambar.',
    'Rice, urad dal, sambar lentils, vegetables',
    90.00, NULL, 10, 'VEG', 'MILD', 1, 0, 0, 2, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_south_indian, 'Uttapam',
    'Thick savory pancake topped with onion and tomato.',
    'Rice batter, onion, tomato, green chili',
    130.00, NULL, 15, 'VEG', 'MEDIUM', 1, 0, 0, 3, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_north_indian, 'Butter Chicken',
    'Tandoori chicken simmered in creamy tomato gravy.',
    'Chicken, tomato, butter, cream, fenugreek',
    320.00, NULL, 25, 'NON_VEG', 'MEDIUM', 1, 1, 1, 1, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_north_indian, 'Dal Makhani',
    'Slow-cooked black lentils with butter and cream.',
    'Black lentils, kidney beans, butter, cream',
    210.00, NULL, 30, 'VEG', 'MILD', 1, 0, 0, 2, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_north_indian, 'Paneer Butter Masala',
    'Cottage cheese in rich, mildly spiced tomato gravy.',
    'Paneer, tomato, butter, cashew paste',
    260.00, 240.00, 20, 'VEG', 'MEDIUM', 1, 1, 0, 3, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_chinese, 'Veg Fried Rice',
    'Wok-tossed rice with fresh vegetables.',
    'Rice, carrot, beans, spring onion, soy sauce',
    190.00, NULL, 15, 'VEG', 'MEDIUM', 1, 0, 0, 1, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_chinese, 'Chicken Manchurian',
    'Indo-Chinese fried chicken in tangy sauce.',
    'Chicken, garlic, soy sauce, spring onion',
    270.00, NULL, 20, 'NON_VEG', 'HOT', 1, 0, 0, 2, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_chinese, 'Chilli Paneer',
    'Cottage cheese tossed in spicy Indo-Chinese sauce.',
    'Paneer, capsicum, soy sauce, red chili',
    230.00, NULL, 18, 'VEG', 'HOT', 1, 0, 0, 3, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_beverages, 'Masala Chai',
    'Spiced Indian milk tea.',
    'Tea leaves, milk, cardamom, ginger',
    40.00, NULL, 5, 'VEG', 'MILD', 1, 0, 0, 1, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_beverages, 'Fresh Lime Soda',
    'Chilled soda with fresh lime, sweet or salted.',
    'Lime, soda water, sugar/salt',
    60.00, NULL, 5, 'VEG', 'MILD', 1, 0, 0, 2, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_beverages, 'Cold Coffee',
    'Blended chilled coffee with milk and ice cream.',
    'Coffee, milk, sugar, ice cream',
    90.00, NULL, 8, 'VEG', 'MILD', 1, 0, 0, 3, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_desserts, 'Gulab Jamun',
    'Soft milk-solid dumplings soaked in rose-cardamom syrup.',
    'Milk solids, sugar syrup, cardamom, rose water',
    80.00, NULL, 5, 'VEG', 'MILD', 1, 0, 1, 1, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

INSERT INTO food_item
    (restaurant_id, category_id, name, description, ingredients, price, offer_price,
     prep_time_minutes, food_type, spice_level, is_available, is_recommended, is_bestseller,
     display_order, created_at, updated_at)
SELECT * FROM (SELECT
    @restaurant_id, @cat_desserts, 'Chocolate Brownie',
    'Warm fudge brownie served with a scoop of vanilla ice cream.',
    'Chocolate, flour, butter, vanilla ice cream',
    150.00, NULL, 10, 'VEG', 'MILD', 1, 1, 0, 2, NOW(), NOW()
) AS t WHERE @already_seeded = 0;

-- -----------------------------------------------------------------------------
-- tax (CGST + SGST, 2.5% each = 5% total)
-- -----------------------------------------------------------------------------
INSERT INTO tax (restaurant_id, name, percent, is_active, created_at)
SELECT @restaurant_id, 'CGST', 2.50, 1, NOW() WHERE @already_seeded = 0;
INSERT INTO tax (restaurant_id, name, percent, is_active, created_at)
SELECT @restaurant_id, 'SGST', 2.50, 1, NOW() WHERE @already_seeded = 0;

-- -----------------------------------------------------------------------------
-- discount (one sample coupon code)
-- -----------------------------------------------------------------------------
INSERT INTO discount (restaurant_id, code, description, discount_type, value, is_active, created_at)
SELECT @restaurant_id, 'WELCOME10', 'First visit 10% off', 'PERCENT', 10.00, 1, NOW() WHERE @already_seeded = 0;

-- -----------------------------------------------------------------------------
-- Confirm what happened
-- -----------------------------------------------------------------------------
SELECT
    CASE WHEN @already_seeded > 0
        THEN 'Skipped - "Starters" category already existed for this restaurant.'
        ELSE 'Seeded: 6 categories, 17 food items, 2 taxes, 1 discount.'
    END AS result;
