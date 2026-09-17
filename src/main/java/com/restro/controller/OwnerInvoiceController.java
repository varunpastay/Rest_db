package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.Repo.PaymentRepository;
import com.restro.entity.Order;
import com.restro.entity.Payment;
import com.restro.entity.Restaurant;
import com.restro.entity.TableSession;
import com.restro.Service.RestaurantService;
import com.restro.Service.TableSessionService;
import com.restro.util.PdfInvoiceUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Combined PDF invoice for a whole table session - one document listing
 * every item from every order in that session, whether it's still open
 * (a "preview bill" before the owner has settled it) or already paid (the
 * final receipt, with the payment method shown).
 */
@RestController
public class OwnerInvoiceController {

    @Autowired
    private TableSessionService tableSessionService;
    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PdfInvoiceUtil pdfInvoiceUtil;

    @GetMapping("/owner/table-sessions/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Integer id) throws Exception {
        TableSession session = tableSessionService.get(id);
        List<Order> orders = tableSessionService.ordersInSession(id);
        Restaurant restaurant = restaurantService.getRestaurant();
        Payment payment = paymentRepository.findByTableSession_TableSessionId(id).orElse(null);

        byte[] bytes = pdfInvoiceUtil.generateInvoice(session, orders, restaurant, payment);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-table-" + session.getTable().getTableNo() + ".pdf")
                .body(bytes);
    }
}
