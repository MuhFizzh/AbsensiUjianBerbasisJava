package com.wms.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.wms.model.InboundHeader;
import com.wms.model.InboundItem;
import com.wms.model.LokasiRak;
import com.wms.model.OutboundHeader;
import com.wms.model.OutboundItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/** Membuat dokumen operasional siap cetak dalam PDF. */
public final class DocumentPrintService {
    private DocumentPrintService() {}

    public static void printOutbound(Component parent, OutboundHeader h, List<OutboundItem> items) {
        String customer = h.getCustomerNama() == null ? "-" : h.getCustomerNama();
        createAndOpen(parent, "Surat Jalan " + h.getNomor(),
                "SURAT JALAN OUTBOUND", h.getNomor(), h.getTanggal(),
                "Customer", customer, items.stream().map(i -> new String[]{
                        i.getBarangKode(), i.getBarangNama(), i.getLokasiKode(), String.valueOf(i.getQty())}).toList());
    }

    public static void printInbound(Component parent, InboundHeader h, List<InboundItem> items) {
        String supplier = h.getSupplierNama() == null ? "-" : h.getSupplierNama();
        createAndOpen(parent, "Tanda Terima " + h.getNomor(),
                "TANDA TERIMA INBOUND", h.getNomor(), h.getTanggal(),
                "Supplier", supplier, items.stream().map(i -> new String[]{
                        i.getBarangKode(), i.getBarangNama(), i.getLokasiKode(), String.valueOf(i.getQty())}).toList());
    }

    private static void createAndOpen(Component parent, String filename, String title, String nomor,
                                      LocalDate tanggal, String partyLabel, String party,
                                      List<String[]> rows) {
        try {
            File dir = new File(System.getProperty("user.home"), "Downloads/WMS-Print");
            Files.createDirectories(dir.toPath());
            File file = new File(dir, filename.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf");
            try (PDDocument doc = new PDDocument()) {
                PDRectangle pageSize = PDRectangle.A4;
                PDPage page = new PDPage(pageSize);
                doc.addPage(page);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float x = 45, y = 785;
                    text(cs, bold, 18, x, y, title);
                    y -= 28;
                    text(cs, regular, 10, x, y, "Nomor: " + nomor);
                    text(cs, regular, 10, 330, y, "Tanggal: " + (tanggal == null ? "-" : tanggal));
                    y -= 18;
                    text(cs, regular, 10, x, y, partyLabel + ": " + party);
                    y -= 28;
                    String[] headers = {"Kode", "Barang", "Lokasi", "Qty"};
                    float[] widths = {75, 230, 90, 60};
                    drawRow(cs, headers, bold, x, y, widths, true);
                    y -= 20;
                    int total = 0;
                    for (String[] row : rows) {
                        if (y < 65) break;
                        drawRow(cs, row, regular, x, y, widths, false);
                        y -= 20;
                        try { total += Integer.parseInt(row[3]); } catch (Exception ignored) {}
                    }
                    y -= 10;
                    text(cs, bold, 10, x, y, "Total Qty: " + total);
                    y -= 45;
                    text(cs, regular, 10, x, y, "Catatan: dokumen ini dibuat oleh sistem WMS.");
                    text(cs, regular, 10, x, y - 45, "Petugas, ____________________");
                    text(cs, regular, 10, 360, y - 45, "Penerima, ____________________");
                }
                doc.save(file);
            }
            openFile(file);
            JOptionPane.showMessageDialog(parent,
                    "Dokumen siap dicetak:\n" + file.getAbsolutePath(),
                    "Dokumen Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Gagal membuat dokumen:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void printRackLabel(Component parent, LokasiRak lokasi) {
        try {
            File dir = new File(System.getProperty("user.home"), "Downloads/WMS-Print");
            Files.createDirectories(dir.toPath());
            File file = new File(dir, "Label-Rak-" + lokasi.getKode().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf");
            BitMatrix matrix = new MultiFormatWriter().encode(lokasi.getKode(), BarcodeFormat.CODE_128, 520, 150);
            BufferedImage barcode = new BufferedImage(520, 150, BufferedImage.TYPE_INT_RGB);
            for (int xx = 0; xx < 520; xx++) for (int yy = 0; yy < 150; yy++)
                barcode.setRGB(xx, yy, matrix.get(xx, yy) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(new PDRectangle(288, 216));
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    text(cs, bold, 20, 30, 185, "LOKASI RAK");
                    text(cs, bold, 30, 30, 150, lokasi.getKode());
                    text(cs, regular, 10, 30, 130, "Area: " + safe(lokasi.getArea()));
                    text(cs, regular, 10, 30, 115, "Rak: " + safe(lokasi.getRak()) + "  Kolom: " + safe(lokasi.getKolom()));
                    text(cs, regular, 10, 30, 100, "Posisi: " + safe(lokasi.getPosisi()));
                    cs.drawImage(LosslessFactory.createFromImage(doc, barcode), 30, 25, 228, 65);
                    text(cs, regular, 9, 95, 17, lokasi.getKode());
                }
                doc.save(file);
            }
            openFile(file);
            JOptionPane.showMessageDialog(parent,
                    "Label barcode siap dicetak:\n" + file.getAbsolutePath(),
                    "Label Berhasil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Gagal membuat label barcode:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void drawRow(PDPageContentStream cs, String[] values, PDType1Font font,
                                float x, float y, float[] widths, boolean header) throws Exception {
        float cx = x;
        for (int i = 0; i < widths.length; i++) {
            cs.addRect(cx, y - 16, widths[i], 18);
            cs.stroke();
            String v = i < values.length ? values[i] : "";
            text(cs, font, header ? 8.5f : 8, cx + 3, y - 11, clip(v, i == 1 ? 35 : 15));
            cx += widths[i];
        }
    }

    private static void text(PDPageContentStream cs, PDType1Font font, float size,
                             float x, float y, String value) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(value));
        cs.endText();
    }

    private static String safe(String s) { return s == null ? "-" : s; }
    private static String clip(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }
    private static String sanitize(String s) { return s == null ? "" : s.replaceAll("[^\\x20-\\x7E]", "?"); }
    private static void openFile(File file) throws Exception {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
    }
}
