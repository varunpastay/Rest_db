package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.Repo.OrderRepository;
import com.restro.entity.Discount;
import com.restro.entity.Restaurant;
import com.restro.entity.Tax;
import com.restro.Repo.DiscountRepository;
import com.restro.Repo.TaxRepository;
import com.restro.Service.FileStorageService;
import com.restro.Service.RestaurantService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;

/** Restaurant settings: branding, contact, currency, taxes, service charge, discounts, business hours, open/closed toggle. */
@Controller
@RequestMapping("/owner/settings")
public class OwnerSettingsController {

    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private TaxRepository taxRepository;
    @Autowired
    private DiscountRepository discountRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public String settings(Model model) {
        Restaurant restaurant = restaurantService.getRestaurant();
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("taxes", taxRepository.findByRestaurant_RestaurantIdOrderByNameAsc(restaurant.getRestaurantId()));
        model.addAttribute("discounts", discountRepository.findByRestaurant_RestaurantIdOrderByCreatedAtDesc(restaurant.getRestaurantId()));
        return "owner/settings";
    }

    @PostMapping("/general")
    public String saveGeneral(@RequestParam String name, @RequestParam(required = false) String address,
                               @RequestParam(required = false) String phone, @RequestParam(required = false) String email,
                               @RequestParam(required = false) String gstin,
                               @RequestParam(required = false) String upiId,
                               @RequestParam(defaultValue = "INR") String currencyCode,
                               @RequestParam(defaultValue = "\u20B9") String currencySymbol,
                               @RequestParam(defaultValue = "0") BigDecimal serviceChargePercent,
                               @RequestParam(defaultValue = "#c0392b") String themeColor,
                               @RequestParam(required = false) String openingTime,
                               @RequestParam(required = false) String closingTime,
                               @RequestParam(required = false, defaultValue = "false") boolean open,
                               @RequestParam(required = false) MultipartFile logo,
                               @RequestParam(required = false) MultipartFile banner) throws IOException {
        Restaurant restaurant = restaurantService.getRestaurant();
        restaurant.setName(name);
        restaurant.setAddress(address);
        restaurant.setPhone(phone);
        restaurant.setEmail(email);
        restaurant.setGstin(gstin);
        restaurant.setUpiId(upiId);
        restaurant.setCurrencyCode(currencyCode);
        restaurant.setCurrencySymbol(currencySymbol);
        restaurant.setServiceChargePercent(serviceChargePercent);
        restaurant.setThemeColor(themeColor);
        restaurant.setOpeningTime(openingTime != null && !openingTime.isBlank() ? LocalTime.parse(openingTime) : null);
        restaurant.setClosingTime(closingTime != null && !closingTime.isBlank() ? LocalTime.parse(closingTime) : null);
        restaurant.setOpen(open);
        if (logo != null && !logo.isEmpty()) {
            restaurant.setLogoPath(fileStorageService.store(logo, "branding"));
        }
        if (banner != null && !banner.isEmpty()) {
            restaurant.setBannerPath(fileStorageService.store(banner, "branding"));
        }
        restaurantService.save(restaurant);
        return "redirect:/owner/settings";
    }

    @PostMapping("/open-toggle")
    @ResponseBody
    public String toggleOpen() {
        Restaurant restaurant = restaurantService.getRestaurant();
        restaurant.setOpen(!restaurant.isOpen());
        restaurantService.save(restaurant);
        return restaurant.isOpen() ? "open" : "closed";
    }

    @PostMapping("/tax")
    public String saveTax(@RequestParam(required = false) Integer taxId, @RequestParam String name,
                           @RequestParam BigDecimal percent,
                           @RequestParam(required = false, defaultValue = "true") boolean active) {
        Restaurant restaurant = restaurantService.getRestaurant();
        Tax tax = taxId != null ? taxRepository.findById(taxId).orElseThrow()
                : Tax.builder().restaurant(restaurant).build();
        tax.setName(name);
        tax.setPercent(percent);
        tax.setActive(active);
        taxRepository.save(tax);
        return "redirect:/owner/settings";
    }

    /** Tax has no incoming foreign key from Order (the tax amount is computed and stored on the
     *  order at checkout time, not linked back to the Tax row), so this delete is always safe -
     *  the try/catch is just cheap insurance against a future schema change. */
    @PostMapping("/tax/{id}/delete")
    public String deleteTax(@PathVariable Integer id) {
        try {
            taxRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            return "redirect:/owner/settings?deleteError=" + java.net.URLEncoder.encode(
                    "This tax can't be deleted because it's still referenced elsewhere.", java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/owner/settings";
    }

    @PostMapping("/discount")
    public String saveDiscount(@RequestParam(required = false) Integer discountId, @RequestParam String code,
                                @RequestParam(required = false) String description,
                                @RequestParam(defaultValue = "PERCENT") String discountType,
                                @RequestParam BigDecimal value,
                                @RequestParam(required = false, defaultValue = "true") boolean active) {
        Restaurant restaurant = restaurantService.getRestaurant();
        Discount discount = discountId != null ? discountRepository.findById(discountId).orElseThrow()
                : Discount.builder().restaurant(restaurant).build();
        discount.setCode(code);
        discount.setDescription(description);
        discount.setDiscountType(com.restro.entity.DiscountType.valueOf(discountType));
        discount.setValue(value);
        discount.setActive(active);
        discountRepository.save(discount);
        return "redirect:/owner/settings";
    }

    /** order.discount_id is a foreign key - once a discount has actually been used on an order,
     *  the DB refuses to delete it. Check first and give a clear message rather than a raw SQL
     *  error; deactivating (the Active toggle in the edit form) is the right move for a
     *  discount that's been used historically and shouldn't be offered any more. */
    @PostMapping("/discount/{id}/delete")
    public String deleteDiscount(@PathVariable Integer id) {
        if (orderRepository.existsByDiscount_DiscountId(id)) {
            return "redirect:/owner/settings?deleteError=" + java.net.URLEncoder.encode(
                    "This discount has already been used on an order and can't be deleted. Mark it inactive instead.",
                    java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            discountRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            return "redirect:/owner/settings?deleteError=" + java.net.URLEncoder.encode(
                    "This discount can't be deleted because it's still referenced elsewhere.", java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/owner/settings";
    }
}
