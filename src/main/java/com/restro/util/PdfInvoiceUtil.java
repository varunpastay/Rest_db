package com.restro.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import com.restro.entity.Order;
import com.restro.entity.OrderItem;
import com.restro.entity.Payment;
import com.restro.entity.Restaurant;
import com.restro.entity.TableSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * One combined invoice for every order in a table's session, printed as a compact receipt -
 * courier typeface, dotted rules, centered header/footer - the same look as a classic
 * point-of-sale bill. If the restaurant has a UPI ID configured (Settings > General), the
 * invoice ends with a scan-to-pay QR pre-filled with the exact grand total.
 */
@Component
public class PdfInvoiceUtil {

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    /**
     * @param session the table session (still open = preview bill; payment != null = settled receipt)
     */
    public byte[] generateInvoice(TableSession session, List<Order> orders, Restaurant restaurant, Payment payment)
            throws DocumentException {
        Document doc = new Document(PageSize.A5, 22, 22, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font nameFont = new Font(Font.COURIER, 15, Font.BOLD);
        Font tagFont = new Font(Font.COURIER, 8, Font.NORMAL);
        Font titleFont = new Font(Font.COURIER, 10, Font.BOLD);
        Font normal = new Font(Font.COURIER, 9, Font.NORMAL);
        Font bold = new Font(Font.COURIER, 9, Font.BOLD);
        Font small = new Font(Font.COURIER, 7, Font.ITALIC);
        Font grandFont = new Font(Font.COURIER, 11, Font.BOLD);

        // ---- Header ----
        doc.add(center(restaurant.getName().toUpperCase(), nameFont));
        if (restaurant.getAddress() != null && !restaurant.getAddress().isBlank()) {
            doc.add(center(restaurant.getAddress(), tagFont));
        }
        if (restaurant.getPhone() != null && !restaurant.getPhone().isBlank()) {
            doc.add(center("Ph: " + restaurant.getPhone(), tagFont));
        }
        if (restaurant.getGstin() != null && !restaurant.getGstin().isBlank()) {
            doc.add(center("GSTIN: " + restaurant.getGstin(), tagFont));
        }
        doc.add(center(payment != null ? "TAX INVOICE" : "BILL (PREVIEW - NOT YET PAID)", titleFont));
        doc.add(dottedRule());

        // ---- Bill meta ----
        String billNo = payment != null ? payment.getInvoiceNo() : "PREVIEW";
        String dateStr = session.getOpenedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeStr = session.getOpenedAt().format(DateTimeFormatter.ofPattern("hh:mm a"));
        doc.add(metaRow("Bill No :", billNo, "Date :", dateStr, normal, bold));
        doc.add(metaRow("Table :", "#" + session.getTable().getTableNo(), "Time :", timeStr, normal, bold));
        doc.add(new Paragraph("Orders : " + orders.stream().map(Order::getOrderNo)
                .reduce((a, b) -> a + ", " + b).orElse("-"), small));
        doc.add(dottedRule());

        // ---- Items ----
        PdfPTable table = new PdfPTable(new float[]{4.2f, 0.8f, 1.4f, 1.6f});
        table.setWidthPercentage(100);
        table.addCell(headerCell("Item", normal));
        table.addCell(headerCell("Qty", normal));
        table.addCell(headerCell("Rate", normal));
        table.addCell(headerCell("Amt", normal));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal serviceChargeAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        int totalQty = 0;

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Phrase itemPhrase = new Phrase(item.getFoodNameSnapshot(), normal);
                if (item.getSpecialInstructions() != null && !item.getSpecialInstructions().isBlank()) {
                    itemPhrase.add(new Chunk("\n" + item.getSpecialInstructions(), small));
                }
                table.addCell(plainCell(itemPhrase, Element.ALIGN_LEFT));
                table.addCell(plainCell(new Phrase(String.valueOf(item.getQuantity()), normal), Element.ALIGN_CENTER));
                table.addCell(plainCell(new Phrase(item.getUnitPrice().toString(), normal), Element.ALIGN_RIGHT));
                table.addCell(plainCell(new Phrase(item.getLineTotal().toString(), normal), Element.ALIGN_RIGHT));
                totalQty += item.getQuantity();
            }
            subtotal = subtotal.add(order.getSubtotal());
            taxAmount = taxAmount.add(order.getTaxAmount());
            serviceChargeAmount = serviceChargeAmount.add(order.getServiceChargeAmount());
            discountAmount = discountAmount.add(order.getDiscountAmount());
            grandTotal = grandTotal.add(order.getGrandTotal());
        }
        doc.add(table);
        doc.add(dottedRule());

        for (Order order : orders) {
            if (order.getCustomerNote() != null && !order.getCustomerNote().isBlank()) {
                doc.add(new Paragraph(order.getOrderNo() + " note: " + order.getCustomerNote(), small));
            }
        }

        String cur = restaurant.getCurrencySymbol();
        doc.add(totalsRow("Total Qty", String.valueOf(totalQty), normal));
        doc.add(totalsRow("Subtotal", cur + subtotal, normal));
        doc.add(totalsRow("Tax", cur + taxAmount, normal));
        if (serviceChargeAmount.signum() > 0) {
            doc.add(totalsRow("Service Charge", cur + serviceChargeAmount, normal));
        }
        if (discountAmount.signum() > 0) {
            doc.add(totalsRow("Discount", "-" + cur + discountAmount, normal));
        }
        doc.add(dottedRule());
        doc.add(totalsRow("NET PAYABLE", cur + grandTotal, grandFont));
        doc.add(dottedRule());

        doc.add(new Paragraph(NumberToWordsUtil.rupeesInWords(grandTotal), small));
        doc.add(Chunk.NEWLINE);

        if (payment != null) {
            doc.add(new Paragraph("Payment : " + payment.getMethod(), normal));
        } else {
            doc.add(new Paragraph("Payment : Not yet settled", normal));
        }
        doc.add(dottedRule());

        // ---- Declaration + UPI QR ----
        doc.add(center("Thank you for dining with us!", normal));
        doc.add(center("Visit us again.", small));

        if (restaurant.getUpiId() != null && !restaurant.getUpiId().isBlank()) {
            try {
                String upiUri = "upi://pay?pa=" + URLEncoder.encode(restaurant.getUpiId(), StandardCharsets.UTF_8)
                        + "&pn=" + URLEncoder.encode(restaurant.getName(), StandardCharsets.UTF_8)
                        + "&am=" + URLEncoder.encode(grandTotal.toString(), StandardCharsets.UTF_8)
                        + "&cu=" + restaurant.getCurrencyCode()
                        + "&tn=" + URLEncoder.encode("Bill " + billNo, StandardCharsets.UTF_8);
                byte[] qrPng = qrCodeGenerator.generatePng(upiUri, 220);
                Image qrImage = Image.getInstance(qrPng);
                qrImage.scaleToFit(110, 110);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                doc.add(Chunk.NEWLINE);
                doc.add(qrImage);
                doc.add(center("Scan to pay " + cur + grandTotal + " via UPI", small));
            } catch (Exception ignored) {
                // If QR generation fails for any reason, the invoice still prints fine without it.
            }
        }

        doc.close();
        return out.toByteArray();
    }

    private Paragraph center(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private Element dottedRule() {
        DottedLineSeparator line = new DottedLineSeparator();
        line.setGap(2f);
        Paragraph p = new Paragraph();
        p.add(new Chunk(line));
        p.setSpacingBefore(2f);
        p.setSpacingAfter(2f);
        return p;
    }

    private Paragraph metaRow(String label1, String value1, String label2, String value2, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label1 + " ", labelFont));
        p.add(new Chunk(value1 + "     ", valueFont));
        p.add(new Chunk(label2 + " ", labelFont));
        p.add(new Chunk(value2, valueFont));
        return p;
    }

    /** A borderless two-column row (label left, value right) for the totals block. Returned as
     *  its own small table so the amount lines up in a straight right-hand column like a real
     *  receipt, rather than drifting with label length. */
    private PdfPTable totalsRow(String label, String value, Font font) {
        PdfPTable t = new PdfPTable(new float[]{2f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(1f);
        PdfPCell left = new PdfPCell(new Phrase(label, font));
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(1f);
        PdfPCell right = new PdfPCell(new Phrase(value, font));
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setPadding(1f);
        t.addCell(left);
        t.addCell(right);
        return t;
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setPaddingBottom(3f);
        return cell;
    }

    private PdfPCell plainCell(Phrase phrase, int align) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPaddingTop(2f);
        cell.setPaddingBottom(2f);
        return cell;
    }
}
