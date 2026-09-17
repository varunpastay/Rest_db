package com.restro.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.restro.Service.ReportService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
public class PdfReportUtil {

    public byte[] toPdf(ReportService.ReportResult report, String rangeLabel, String restaurantName) throws DocumentException {
        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font h2 = new Font(Font.HELVETICA, 13, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);

        doc.add(new Paragraph(restaurantName + " - Sales Report", titleFont));
        doc.add(new Paragraph(rangeLabel, normal));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Summary", h2));
        doc.add(new Paragraph("Total Revenue: " + report.getTotalRevenue(), normal));
        doc.add(new Paragraph("Order Count: " + report.getOrderCount(), normal));
        doc.add(new Paragraph("Average Order Value: " + report.getAverageOrderValue(), normal));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Top Selling Items", h2));
        doc.add(barTable(report.getTopSellerBars(), "Item", "Sold"));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Least Selling Items", h2));
        PdfPTable least = new PdfPTable(2);
        least.setWidthPercentage(100);
        least.addCell("Item");
        least.addCell("Quantity Sold");
        for (Map.Entry<String, Integer> e : report.getLeastSellers()) {
            least.addCell(e.getKey());
            least.addCell(String.valueOf(e.getValue()));
        }
        doc.add(least);
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Peak Hours (by revenue)", h2));
        doc.add(barTable(report.getPeakHourBars(), "Hour Range", "Revenue"));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Revenue Trend", h2));
        doc.add(barTable(report.getRevenueTrendBars(), "Day", "Revenue (Orders)"));

        doc.close();
        return out.toByteArray();
    }

    private PdfPTable barTable(Iterable<ReportService.BarItem> items, String col1, String col2) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(col1);
        table.addCell(col2);
        for (ReportService.BarItem b : items) {
            table.addCell(b.getLabel());
            table.addCell(b.getValueDisplay());
        }
        return table;
    }
}
