package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.entity.QrCode;
import com.restro.entity.Restaurant;
import com.restro.entity.RestaurantTable;
import com.restro.Service.RestaurantService;
import com.restro.Service.TableService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

/** Table & QR management - replaces TableServlet + QRCodeGenerateServlet + QRCodeDownloadServlet + QRCodePrintServlet. */
@Controller
@RequestMapping("/owner/tables")
public class OwnerTableController {

    @Autowired
    private TableService tableService;
    @Autowired
    private RestaurantService restaurantService;

    @GetMapping
    public String tables(Model model) {
        Restaurant restaurant = restaurantService.getRestaurant();
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("tables", tableService.allTables(restaurant.getRestaurantId()));
        return "owner/tables";
    }

    @PostMapping
    public String createTable(@RequestParam String tableNo, @RequestParam(defaultValue = "4") int capacity)
            throws IOException {
        Restaurant restaurant = restaurantService.getRestaurant();
        RestaurantTable table = tableService.createTable(restaurant, tableNo, capacity);
        tableService.generateQrCode(table);
        return "redirect:/owner/tables";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id) {
        try {
            tableService.deleteTable(id);
            return "redirect:/owner/tables";
        } catch (IllegalStateException e) {
            return "redirect:/owner/tables?deleteError=" +
                    java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id) {
        tableService.toggleActive(id);
        return "redirect:/owner/tables";
    }

    /** Redirects to the generic BLOB image endpoint for this table's latest QR code, so <img>/download links stay simple. */
    @GetMapping("/{id}/qr-image")
    public String qrImage(@PathVariable Integer id) {
        String path = tableService.latestQrCode(id)
                .map(QrCode::getImagePath)
                .orElseThrow(() -> new IllegalArgumentException("No QR code generated yet for this table"));
        return "redirect:/images?path=" + path;
    }

    @PostMapping("/{id}/regenerate-qr")
    public String regenerateQr(@PathVariable Integer id) throws IOException {
        RestaurantTable table = tableService.allTables(restaurantService.getRestaurant().getRestaurantId())
                .stream().filter(t -> t.getTableId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));
        tableService.generateQrCode(table);
        return "redirect:/owner/tables";
    }
}
