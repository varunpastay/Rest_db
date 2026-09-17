package com.restro.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restro.Repo.*;
import com.restro.Service.BackupService;
import com.restro.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Restore is an "upsert import", never a destructive wipe: existing orders
 * hold hard foreign keys to categories/food items/tables, so deleting and
 * recreating those rows on restore would either break historical orders or
 * fail outright on the FK constraint. Instead, restore matches each record
 * by its natural business key (category/food name, table number, tax name,
 * discount code) and updates it in place, creating anything missing. Running
 * a restore twice, or restoring an old backup onto a live restaurant, is
 * safe - it never deletes rows the backup doesn't know about.
 */
@Service
public class BackupServiceImpl implements BackupService {

    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FoodItemRepository foodItemRepository;
    @Autowired private FoodImageRepository foodImageRepository;
    @Autowired private RestaurantTableRepository tableRepository;
    @Autowired private TaxRepository taxRepository;
    @Autowired private DiscountRepository discountRepository;
    @Autowired private com.restro.Service.FileStorageService fileStorageService;
    @Autowired private com.restro.Service.TableService tableService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // ---------------- export ----------------

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBackup() {
        Restaurant restaurant = restaurantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No restaurant configured yet"));

        BackupData data = new BackupData();
        data.exportedAt = LocalDateTime.now().toString();

        data.restaurant = new RestaurantDto();
        data.restaurant.name = restaurant.getName();
        data.restaurant.address = restaurant.getAddress();
        data.restaurant.phone = restaurant.getPhone();
        data.restaurant.email = restaurant.getEmail();
        data.restaurant.gstin = restaurant.getGstin();
        data.restaurant.currencyCode = restaurant.getCurrencyCode();
        data.restaurant.currencySymbol = restaurant.getCurrencySymbol();
        data.restaurant.serviceChargePercent = restaurant.getServiceChargePercent();
        data.restaurant.themeColor = restaurant.getThemeColor();
        data.restaurant.openingTime = restaurant.getOpeningTime();
        data.restaurant.closingTime = restaurant.getClosingTime();
        data.restaurant.logoFileKey = embedFile(restaurant.getLogoPath(), data.files);
        data.restaurant.bannerFileKey = embedFile(restaurant.getBannerPath(), data.files);

        for (Category cat : categoryRepository.findByRestaurant_RestaurantIdOrderByDisplayOrderAsc(restaurant.getRestaurantId())) {
            CategoryDto dto = new CategoryDto();
            dto.name = cat.getName();
            dto.displayOrder = cat.getDisplayOrder();
            dto.active = cat.isActive();
            data.categories.add(dto);

            for (FoodItem food : foodItemRepository.findByCategory_CategoryIdOrderByDisplayOrderAsc(cat.getCategoryId())) {
                FoodItemDto foodDto = new FoodItemDto();
                foodDto.categoryName = cat.getName();
                foodDto.name = food.getName();
                foodDto.description = food.getDescription();
                foodDto.ingredients = food.getIngredients();
                foodDto.price = food.getPrice();
                foodDto.offerPrice = food.getOfferPrice();
                foodDto.prepTimeMinutes = food.getPrepTimeMinutes();
                foodDto.foodType = food.getFoodType();
                foodDto.spiceLevel = food.getSpiceLevel();
                foodDto.available = food.isAvailable();
                foodDto.recommended = food.isRecommended();
                foodDto.bestseller = food.isBestseller();
                foodDto.displayOrder = food.getDisplayOrder();
                for (FoodImage img : food.getImages()) {
                    String key = embedFile(img.getImagePath(), data.files);
                    if (key != null) {
                        foodDto.imageFileKeys.add(key);
                    }
                }
                data.foodItems.add(foodDto);
            }
        }

        for (RestaurantTable table : tableRepository.findByRestaurant_RestaurantIdOrderByTableNoAsc(restaurant.getRestaurantId())) {
            TableDto dto = new TableDto();
            dto.tableNo = table.getTableNo();
            dto.capacity = table.getCapacity();
            dto.active = table.isActive();
            data.tables.add(dto);
        }

        for (Tax tax : taxRepository.findByRestaurant_RestaurantIdOrderByNameAsc(restaurant.getRestaurantId())) {
            TaxDto dto = new TaxDto();
            dto.name = tax.getName();
            dto.percent = tax.getPercent();
            dto.active = tax.isActive();
            data.taxes.add(dto);
        }

        for (Discount discount : discountRepository.findByRestaurant_RestaurantIdOrderByCreatedAtDesc(restaurant.getRestaurantId())) {
            DiscountDto dto = new DiscountDto();
            dto.code = discount.getCode();
            dto.description = discount.getDescription();
            dto.discountType = discount.getDiscountType();
            dto.value = discount.getValue();
            dto.active = discount.isActive();
            data.discounts.add(dto);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize backup", e);
        }
    }

    private String embedFile(String relativePath, Map<String, FileDto> files) {
        if (relativePath == null) {
            return null;
        }
        return fileStorageService.find(relativePath).map(f -> {
            FileDto dto = new FileDto();
            dto.contentType = f.getContentType();
            dto.base64Data = Base64.getEncoder().encodeToString(f.getData());
            files.put(relativePath, dto);
            return relativePath;
        }).orElse(null);
    }

    // ---------------- restore ----------------

    @Override
    @Transactional
    public void restoreBackup(byte[] backupJson) throws IOException {
        BackupData data = objectMapper.readValue(backupJson, BackupData.class);

        Restaurant restaurant = restaurantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No restaurant configured yet"));

        if (data.restaurant != null) {
            RestaurantDto r = data.restaurant;
            restaurant.setName(r.name);
            restaurant.setAddress(r.address);
            restaurant.setPhone(r.phone);
            restaurant.setEmail(r.email);
            restaurant.setGstin(r.gstin);
            restaurant.setCurrencyCode(r.currencyCode);
            restaurant.setCurrencySymbol(r.currencySymbol);
            restaurant.setServiceChargePercent(r.serviceChargePercent);
            restaurant.setThemeColor(r.themeColor);
            restaurant.setOpeningTime(r.openingTime);
            restaurant.setClosingTime(r.closingTime);
            if (r.logoFileKey != null) {
                restaurant.setLogoPath(restoreFile(r.logoFileKey, data.files, "branding"));
            }
            if (r.bannerFileKey != null) {
                restaurant.setBannerPath(restoreFile(r.bannerFileKey, data.files, "branding"));
            }
            restaurantRepository.save(restaurant);
        }

        Map<String, Category> categoryByName = new HashMap<>();
        for (Category existing : categoryRepository.findByRestaurant_RestaurantIdOrderByDisplayOrderAsc(restaurant.getRestaurantId())) {
            categoryByName.put(existing.getName().toLowerCase(), existing);
        }
        for (CategoryDto dto : data.categories) {
            Category cat = categoryByName.get(dto.name.toLowerCase());
            if (cat == null) {
                cat = Category.builder().restaurant(restaurant).name(dto.name).build();
            }
            cat.setDisplayOrder(dto.displayOrder);
            cat.setActive(dto.active);
            cat = categoryRepository.save(cat);
            categoryByName.put(dto.name.toLowerCase(), cat);
        }

        for (FoodItemDto dto : data.foodItems) {
            Category cat = categoryByName.get(dto.categoryName.toLowerCase());
            if (cat == null) {
                continue; // category missing from this backup - skip rather than guess
            }
            FoodItem food = foodItemRepository.findByCategory_CategoryIdOrderByDisplayOrderAsc(cat.getCategoryId())
                    .stream().filter(f -> f.getName().equalsIgnoreCase(dto.name)).findFirst()
                    .orElseGet(() -> FoodItem.builder().restaurant(restaurant).category(cat).build());
            food.setCategory(cat);
            food.setName(dto.name);
            food.setDescription(dto.description);
            food.setIngredients(dto.ingredients);
            food.setPrice(dto.price);
            food.setOfferPrice(dto.offerPrice);
            food.setPrepTimeMinutes(dto.prepTimeMinutes);
            food.setFoodType(dto.foodType);
            food.setSpiceLevel(dto.spiceLevel);
            food.setAvailable(dto.available);
            food.setRecommended(dto.recommended);
            food.setBestseller(dto.bestseller);
            food.setDisplayOrder(dto.displayOrder);
            FoodItem saved = foodItemRepository.save(food);

            if (saved.getImages().isEmpty() && !dto.imageFileKeys.isEmpty()) {
                boolean first = true;
                for (String key : dto.imageFileKeys) {
                    String path = restoreFile(key, data.files, "food");
                    if (path == null) continue;
                    foodImageRepository.save(FoodImage.builder()
                            .foodItem(saved).imagePath(path).primary(first)
                            .displayOrder(saved.getImages().size()).build());
                    first = false;
                }
            }
        }

        for (TableDto dto : data.tables) {
            boolean exists = tableRepository.findByRestaurant_RestaurantIdOrderByTableNoAsc(restaurant.getRestaurantId())
                    .stream().anyMatch(t -> t.getTableNo().equalsIgnoreCase(dto.tableNo));
            if (exists) {
                continue; // table already present (with its own QR token) - leave it untouched
            }
            RestaurantTable table = tableService.createTable(restaurant, dto.tableNo, dto.capacity);
            if (!dto.active) {
                tableService.toggleActive(table.getTableId());
            }
            try {
                tableService.generateQrCode(table);
            } catch (IOException ignored) {
                // QR can always be regenerated later from the Tables screen
            }
        }

        Map<String, Tax> taxByName = new HashMap<>();
        for (Tax existing : taxRepository.findByRestaurant_RestaurantIdOrderByNameAsc(restaurant.getRestaurantId())) {
            taxByName.put(existing.getName().toLowerCase(), existing);
        }
        for (TaxDto dto : data.taxes) {
            Tax tax = taxByName.get(dto.name.toLowerCase());
            if (tax == null) {
                tax = Tax.builder().restaurant(restaurant).name(dto.name).build();
            }
            tax.setPercent(dto.percent);
            tax.setActive(dto.active);
            taxRepository.save(tax);
        }

        Map<String, Discount> discountByCode = new HashMap<>();
        for (Discount existing : discountRepository.findByRestaurant_RestaurantIdOrderByCreatedAtDesc(restaurant.getRestaurantId())) {
            discountByCode.put(existing.getCode().toLowerCase(), existing);
        }
        for (DiscountDto dto : data.discounts) {
            Discount discount = discountByCode.get(dto.code.toLowerCase());
            if (discount == null) {
                discount = Discount.builder().restaurant(restaurant).code(dto.code).build();
            }
            discount.setDescription(dto.description);
            discount.setDiscountType(dto.discountType);
            discount.setValue(dto.value);
            discount.setActive(dto.active);
            discountRepository.save(discount);
        }
    }

    private String restoreFile(String key, Map<String, FileDto> files, String subdir) {
        FileDto file = files.get(key);
        if (file == null) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(file.base64Data);
        String name = UUID.randomUUID() + guessExtension(file.contentType);
        return fileStorageService.storeBytes(bytes, file.contentType, subdir, name);
    }

    private String guessExtension(String contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    // ---------------- DTOs (plain public-field classes - Jackson serializes them with no extra annotations needed) ----------------

    public static class BackupData {
        public int version = 1;
        public String exportedAt;
        public RestaurantDto restaurant;
        public List<CategoryDto> categories = new ArrayList<>();
        public List<FoodItemDto> foodItems = new ArrayList<>();
        public List<TableDto> tables = new ArrayList<>();
        public List<TaxDto> taxes = new ArrayList<>();
        public List<DiscountDto> discounts = new ArrayList<>();
        public Map<String, FileDto> files = new HashMap<>();
    }

    public static class RestaurantDto {
        public String name, address, phone, email, gstin, currencyCode, currencySymbol, themeColor;
        public java.math.BigDecimal serviceChargePercent;
        public LocalTime openingTime, closingTime;
        public String logoFileKey, bannerFileKey;
    }

    public static class CategoryDto {
        public String name;
        public int displayOrder;
        public boolean active;
    }

    public static class FoodItemDto {
        public String categoryName, name, description, ingredients;
        public java.math.BigDecimal price, offerPrice;
        public int prepTimeMinutes;
        public FoodType foodType;
        public SpiceLevel spiceLevel;
        public boolean available, recommended, bestseller;
        public int displayOrder;
        public List<String> imageFileKeys = new ArrayList<>();
    }

    public static class TableDto {
        public String tableNo;
        public int capacity;
        public boolean active;
    }

    public static class TaxDto {
        public String name;
        public java.math.BigDecimal percent;
        public boolean active;
    }

    public static class DiscountDto {
        public String code, description;
        public DiscountType discountType;
        public java.math.BigDecimal value;
        public boolean active;
    }

    public static class FileDto {
        public String contentType;
        public String base64Data;
    }
}
