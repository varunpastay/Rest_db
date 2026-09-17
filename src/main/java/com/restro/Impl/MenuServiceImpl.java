package com.restro.Impl;

import com.restro.Repo.CategoryRepository;
import com.restro.Repo.FoodImageRepository;
import com.restro.Repo.FoodItemRepository;
import com.restro.Repo.OrderItemRepository;
import com.restro.Service.FileStorageService;
import com.restro.Service.MenuService;
import com.restro.entity.Category;
import com.restro.entity.FoodImage;
import com.restro.entity.FoodItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private FoodImageRepository foodImageRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // ---- Customer-facing (read only) ----

    @Override
    @Transactional(readOnly = true)
    public List<Category> activeCategories(Integer restaurantId) {
        return categoryRepository.findByRestaurant_RestaurantIdAndActiveTrueOrderByDisplayOrderAsc(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> availableFoodItems(Integer restaurantId) {
        return foodItemRepository.findByRestaurant_RestaurantIdAndAvailableTrueOrderByDisplayOrderAsc(restaurantId);
    }

    // ---- Owner management ----

    @Override
    @Transactional(readOnly = true)
    public List<Category> allCategories(Integer restaurantId) {
        return categoryRepository.findByRestaurant_RestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> allFoodItems(Integer restaurantId) {
        return foodItemRepository.findByRestaurant_RestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodItem getFoodItem(Integer id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Food item not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    @Override
    @Transactional
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    /**
     * Category has a hard, non-null foreign key from every food item in it
     * (food_item.category_id), so the DB refuses the delete outright while
     * any dish still points at this category. Rather than let that surface
     * as a raw SQL error, check first and refuse with a clear, actionable
     * message.
     */
    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        List<FoodItem> itemsInCategory = foodItemRepository.findByCategory_CategoryIdOrderByDisplayOrderAsc(id);
        if (!itemsInCategory.isEmpty()) {
            throw new IllegalStateException(
                    "This category still has " + itemsInCategory.size() + " menu item(s) in it. "
                            + "Delete or move them to another category first.");
        }
        try {
            categoryRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "This category can't be deleted because it's still referenced elsewhere.", e);
        }
    }

    @Override
    @Transactional
    public FoodItem saveFoodItem(FoodItem item) {
        return foodItemRepository.save(item);
    }

    /**
     * order_item.food_item_id is a hard, non-null foreign key (a historical
     * order line must always be able to say what it was) - so once an item
     * has been ordered even once, the DB refuses to delete it. Rather than
     * let that surface as a raw SQL error, check first and point the owner
     * at "mark unavailable" instead, which is what they actually want for
     * a discontinued dish that still has order history.
     */
    @Override
    @Transactional
    public void deleteFoodItem(Integer id) {
        if (orderItemRepository.existsByFoodItem_FoodItemId(id)) {
            throw new IllegalStateException(
                    "This item has already been ordered and can't be deleted, since past orders still "
                            + "reference it. Use the availability toggle to take it off the menu instead.");
        }
        try {
            foodItemRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "This item can't be deleted because it's still referenced elsewhere. "
                            + "Use the availability toggle instead.", e);
        }
    }

    @Override
    @Transactional
    public void toggleAvailability(Integer foodItemId) {
        FoodItem item = getFoodItem(foodItemId);
        item.setAvailable(!item.isAvailable());
        foodItemRepository.save(item);
    }

    @Override
    @Transactional
    public void addImage(FoodItem foodItem, MultipartFile file, boolean primary) throws IOException {
        String path = fileStorageService.store(file, "food");
        if (primary) {
            foodItem.getImages().forEach(img -> img.setPrimary(false));
        }
        FoodImage image = FoodImage.builder()
                .foodItem(foodItem)
                .imagePath(path)
                .primary(primary || foodItem.getImages().isEmpty())
                .displayOrder(foodItem.getImages().size())
                .build();
        foodImageRepository.save(image);
    }

    /** No incoming foreign key references food_image, so this delete is always safe. Note: there
     *  is currently no owner-facing endpoint that calls this - food items only support replacing/
     *  adding a photo via the menu form, not removing one specific photo from a multi-photo item.
     *  Kept here as ready-to-wire-up capability; see PROJECT_OVERVIEW.md "Known gaps". */
    @Override
    @Transactional
    public void removeImage(Integer foodImageId) {
        foodImageRepository.deleteById(foodImageId);
    }
}
