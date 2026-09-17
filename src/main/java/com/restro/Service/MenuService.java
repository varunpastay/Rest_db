package com.restro.Service;

import com.restro.entity.Category;
import com.restro.entity.FoodItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Category + FoodItem CRUD, used by both the public menu (read-only) and the owner's menu management screens. */
public interface MenuService {

    // ---- Customer-facing (read only) ----
    List<Category> activeCategories(Integer restaurantId);

    List<FoodItem> availableFoodItems(Integer restaurantId);

    // ---- Owner management ----
    List<Category> allCategories(Integer restaurantId);

    List<FoodItem> allFoodItems(Integer restaurantId);

    FoodItem getFoodItem(Integer id);

    Category getCategory(Integer id);

    Category saveCategory(Category category);

    void deleteCategory(Integer id);

    FoodItem saveFoodItem(FoodItem item);

    void deleteFoodItem(Integer id);

    void toggleAvailability(Integer foodItemId);

    void addImage(FoodItem foodItem, MultipartFile file, boolean primary) throws IOException;

    void removeImage(Integer foodImageId);
}
