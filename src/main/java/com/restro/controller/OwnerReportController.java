package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.entity.Restaurant;
import com.restro.Service.ReportService;
import com.restro.Service.RestaurantService;
import com.restro.util.ExcelReportUtil;
import com.restro.util.PdfReportUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Sales Reports: Today/Week/Month/Year, top/least sellers, peak hours, average order value - Excel/PDF export. */
@Controller
public class OwnerReportController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private ExcelReportUtil excelReportUtil;
    @Autowired
    private PdfReportUtil pdfReportUtil;

    @GetMapping("/owner/reports")
    public String reports(@RequestParam(defaultValue = "TODAY") ReportService.Range range, Model model) {
        Restaurant restaurant = restaurantService.getRestaurant();
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("range", range);
        model.addAttribute("report", reportService.generate(restaurant.getRestaurantId(), range));
        return "owner/reports";
    }

    @GetMapping("/owner/reports/export/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(defaultValue = "TODAY") ReportService.Range range) throws Exception {
        Restaurant restaurant = restaurantService.getRestaurant();
        byte[] bytes = excelReportUtil.toExcel(reportService.generate(restaurant.getRestaurantId(), range), range.name());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-report-" + range.name().toLowerCase() + ".xlsx")
                .body(bytes);
    }

    @GetMapping("/owner/reports/export/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam(defaultValue = "TODAY") ReportService.Range range) throws Exception {
        Restaurant restaurant = restaurantService.getRestaurant();
        byte[] bytes = pdfReportUtil.toPdf(reportService.generate(restaurant.getRestaurantId(), range), range.name(), restaurant.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-report-" + range.name().toLowerCase() + ".pdf")
                .body(bytes);
    }
}
