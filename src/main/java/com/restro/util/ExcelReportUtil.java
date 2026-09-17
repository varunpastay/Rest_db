package com.restro.util;

import com.restro.Service.ReportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class ExcelReportUtil {

    public byte[] toExcel(ReportService.ReportResult report, String rangeLabel) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            header.setFont(bold);

            Sheet summary = wb.createSheet("Summary");
            int r = 0;
            row(summary, r++, header, "Sales Report - " + rangeLabel);
            row(summary, r++, null, "Total Revenue", report.getTotalRevenue().toString());
            row(summary, r++, null, "Order Count", String.valueOf(report.getOrderCount()));
            row(summary, r++, null, "Average Order Value", report.getAverageOrderValue().toString());

            Sheet topSellers = wb.createSheet("Top Sellers");
            row(topSellers, 0, header, "Item", "Quantity Sold");
            int i = 1;
            for (ReportService.BarItem b : report.getTopSellerBars()) {
                row(topSellers, i++, null, b.getLabel(), b.getValueDisplay());
            }

            Sheet leastSellers = wb.createSheet("Least Sellers");
            row(leastSellers, 0, header, "Item", "Quantity Sold");
            int k = 1;
            for (Map.Entry<String, Integer> e : report.getLeastSellers()) {
                row(leastSellers, k++, null, e.getKey(), String.valueOf(e.getValue()));
            }

            Sheet hours = wb.createSheet("Peak Hours");
            row(hours, 0, header, "Hour Range", "Revenue");
            int j = 1;
            for (ReportService.BarItem b : report.getPeakHourBars()) {
                row(hours, j++, null, b.getLabel(), b.getValueDisplay());
            }

            Sheet trend = wb.createSheet("Revenue Trend");
            row(trend, 0, header, "Day", "Revenue (Orders)");
            int t = 1;
            for (ReportService.BarItem b : report.getRevenueTrendBars()) {
                row(trend, t++, null, b.getLabel(), b.getValueDisplay());
            }

            for (Sheet s : List.of(summary, topSellers, leastSellers, hours, trend)) {
                for (int c = 0; c < 3; c++) {
                    s.autoSizeColumn(c);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void row(Sheet sheet, int rowIdx, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int c = 0; c < values.length; c++) {
            Cell cell = row.createCell(c);
            cell.setCellValue(values[c]);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }
}
