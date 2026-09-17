package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.entity.Category;
import com.restro.entity.FoodItem;
import com.restro.entity.Restaurant;
import com.restro.Service.MenuService;
import com.restro.Service.RestaurantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

/** Category + Food Item management - replaces CategoryServlet + FoodItemServlet + FoodItemFormServlet + FoodImageServlet. */
@Controller
@RequestMapping("/owner/menu")
public class OwnerMenuController {

    @Autowired
    private MenuService menuService;
    @Autowired
    private RestaurantService restaurantService;

    @GetMapping
    public String menu(Model model) {
        Restaurant restaurant = restaurantService.getRestaurant();
        java.util.List<Category> categories = menuService.allCategories(restaurant.getRestaurantId());
        java.util.List<FoodItem> foodItems = menuService.allFoodItems(restaurant.getRestaurantId());
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("categories", categories);
        model.addAttribute("foodItems", foodItems);
        // Grouped by category id so the template can just do itemsByCategory.get(cat.categoryId)
        // instead of a SpEL selection expression (list.?[...] can't see the outer th:each variable).
        model.addAttribute("itemsByCategory", foodItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(f -> f.getCategory().getCategoryId())));
        return "owner/menu";
    }

    @PostMapping("/category")
    public String saveCategory(@RequestParam(required = false) Integer categoryId,
                                @RequestParam String name,
                                @RequestParam(defaultValue = "0") int displayOrder) {
        Restaurant restaurant = restaurantService.getRestaurant();
        Category category = categoryId != null ? menuService.getCategory(categoryId)
                : Category.builder().restaurant(restaurant).build();
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        menuService.saveCategory(category);
        return "redirect:/owner/menu";
    }

    @PostMapping("/category/{id}/delete")
    public String deleteCategory(@PathVariable Integer id) {
        try {
            menuService.deleteCategory(id);
        } catch (IllegalStateException e) {
            return "redirect:/owner/menu?deleteError=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/owner/menu";
    }

    @PostMapping("/food-item")
    public String saveFoodItem(@RequestParam(required = false) Integer foodItemId,
                                @RequestParam Integer categoryId,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                @RequestParam(required = false) BigDecimal offerPrice,
                                @RequestParam(defaultValue = "VEG") String foodType,
                                @RequestParam(defaultValue = "MEDIUM") String spiceLevel,
                                @RequestParam(defaultValue = "15") int prepTimeMinutes,
                                @RequestParam(required = false, defaultValue = "false") boolean available,
                                @RequestParam(required = false, defaultValue = "false") boolean recommended,
                                @RequestParam(required = false, defaultValue = "false") boolean bestseller,
                                @RequestParam(required = false) MultipartFile image) throws IOException {
        Restaurant restaurant = restaurantService.getRestaurant();
        FoodItem item = foodItemId != null ? menuService.getFoodItem(foodItemId)
                : FoodItem.builder().restaurant(restaurant).build();
        item.setCategory(menuService.getCategory(categoryId));
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setOfferPrice(offerPrice);
        item.setFoodType(com.restro.entity.FoodType.valueOf(foodType));
        item.setSpiceLevel(com.restro.entity.SpiceLevel.valueOf(spiceLevel));
        item.setPrepTimeMinutes(prepTimeMinutes);
        item.setAvailable(available);
        item.setRecommended(recommended);
        item.setBestseller(bestseller);
        FoodItem saved = menuService.saveFoodItem(item);
        if (image != null && !image.isEmpty()) {
            menuService.addImage(saved, image, saved.getImages().isEmpty());
        }
        return "redirect:/owner/menu";
    }

    @PostMapping("/food-item/{id}/delete")
    public String deleteFoodItem(@PathVariable Integer id) {
        try {
            menuService.deleteFoodItem(id);
        } catch (IllegalStateException e) {
            return "redirect:/owner/menu?deleteError=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/owner/menu";
    }

    @PostMapping("/food-item/{id}/toggle")
    public String toggleAvailability(@PathVariable Integer id) {
        menuService.toggleAvailability(id);
        return "redirect:/owner/menu";
    }
}
