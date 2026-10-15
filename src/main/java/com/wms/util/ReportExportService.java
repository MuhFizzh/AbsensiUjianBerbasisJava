package com.wms.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;

/** Utilitas export laporan generik ke XLSX dan PDF. */
public final class ReportExportService {
    private ReportExportService() {}

    public static void exportExcel(Component parent, String title, String[] columns,
                                   List<Object[]> rows) {
        JFileChooser chooser = chooser(title + ".xlsx");
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(chooser.getSelectedFile(), ".xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(safeSheetName(title));
            Font font = wb.createFont();
            font.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row header = sheet.createRow(0);
            for (int c = 0; c < columns.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(columns[c]);
                cell.setCellStyle(headerStyle);
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Object[] data = rows.get(r);
                for (int c = 0; c < columns.length && c < data.length; c++) {
                    writeCell(row.createCell(c), data[c]);
                }
            }
            sheet.createFreezePane(0, 1);
            for (int c = 0; c < columns.length; c++) sheet.autoSizeColumn(c);
            Files.createDirectories(file.toPath().toAbsolutePath().getParent());
            try (var out = Files.newOutputStream(file.toPath())) { wb.write(out); }
            JOptionPane.showMessageDialog(parent,
                    "Excel berhasil dibuat:\n" + file.getAbsolutePath(),
                    "Export Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(parent, "Gagal export Excel", ex);
        }
    }

    public static void exportPdf(Component parent, String title, String[] columns,
                                 List<Object[]> rows) {
        JFileChooser chooser = chooser(title + ".pdf");
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = ensureExtension(chooser.getSelectedFile(), ".pdf");
        try (PDDocument doc = new PDDocument()) {
            final float margin = 30f;
            PDRectangle pageSize = PDRectangle.A4;
            PDRectangle landscape = new PDRectangle(pageSize.getHeight(), pageSize.getWidth());
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            int pageIndex = 0;
            PDPage page = new PDPage(landscape);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = landscape.getHeight() - margin;
            float usable = landscape.getWidth() - margin * 2;
            float rowH = 18f;
            float colW = usable / Math.max(1, columns.length);
            DecimalFormat df = new DecimalFormat("#,##0");

            for (int i = 0; i < rows.size(); i++) {
                if (y < margin + rowH * 2) {
                    cs.close();
                    page = new PDPage(landscape);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = landscape.getHeight() - margin;
                    pageIndex++;
                }
                if (i == 0 || (i > 0 && y == landscape.getHeight() - margin)) {
                    drawText(cs, title, bold, 14, margin, y);
                    y -= 22;
                    drawHeader(cs, columns, bold, margin, y, colW, rowH);
                    y -= rowH;
                }
                Object[] data = rows.get(i);
                for (int c = 0; c < columns.length; c++) {
                    String value = c < data.length ? formatValue(data[c], df) : "";
                    drawCell(cs, value, regular, 8.5f, margin + c * colW, y, colW, rowH);
                }
                y -= rowH;
                if (i + 1 < rows.size() && y < margin + rowH) {
                    cs.close();
                    page = new PDPage(landscape);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = landscape.getHeight() - margin;
                    drawText(cs, title + " (lanjutan)", bold, 14, margin, y);
                    y -= 22;
                    drawHeader(cs, columns, bold, margin, y, colW, rowH);
                    y -= rowH;
                }
            }
            if (rows.isEmpty()) drawText(cs, title + "\nTidak ada data.", regular, 11, margin, y);
            cs.close();
            doc.save(file);
            JOptionPane.showMessageDialog(parent,
                    "PDF berhasil dibuat:\n" + file.getAbsolutePath(),
                    "Export Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(parent, "Gagal export PDF", ex);
        }
    }

    private static void drawHeader(PDPageContentStream cs, String[] columns, PDType1Font font,
                                   float x, float y, float colW, float rowH) throws IOException {
        for (int c = 0; c < columns.length; c++) drawCell(cs, columns[c], font, 8.5f,
                x + c * colW, y, colW, rowH);
    }

    private static void drawCell(PDPageContentStream cs, String text, PDType1Font font, float size,
                                 float x, float y, float w, float h) throws IOException {
        cs.addRect(x, y - h + 2, w, h);
        cs.stroke();
        drawText(cs, clip(text, Math.max(8, (int)(w / 4.3f))), font, size, x + 3, y - 12);
    }

    private static void drawText(PDPageContentStream cs, String text, PDType1Font font,
                                 float size, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        for (String line : text.split("\\n", -1)) {
            cs.showText(sanitize(line));
            cs.newLineAtOffset(0, -size - 2);
        }
        cs.endText();
    }

    private static String formatValue(Object value, DecimalFormat df) {
        if (value == null) return "";
        if (value instanceof Number n) return df.format(n.doubleValue());
        return String.valueOf(value);
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static JFileChooser chooser(String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        return chooser;
    }

    private static File ensureExtension(File file, String extension) {
        String name = file.getName().toLowerCase();
        return name.endsWith(extension) ? file : new File(file.getParentFile(), file.getName() + extension);
    }

    private static String safeSheetName(String title) {
        String name = title.replaceAll("[\\\\/*?:\\[\\]]", "_");
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

    private static void writeCell(Cell cell, Object value) {
        if (value == null) { cell.setBlank(); return; }
        if (value instanceof Number n) cell.setCellValue(n.doubleValue());
        else cell.setCellValue(String.valueOf(value));
    }

    private static void showError(Component parent, String title, Exception ex) {
        JOptionPane.showMessageDialog(parent, title + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
