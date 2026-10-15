package com.wms.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String generateNextCode(Connection conn, String tableName, String prefix, int digits)
            throws SQLException {
        return generateNextCode(conn, tableName, "kode", prefix, digits);
    }

    /**
     * Sama seperti generateNextCode(conn, tableName, prefix, digits), tapi kolom
     * penampung kode bisa ditentukan sendiri (dipakai untuk kolom "nomor" pada
     * transaksi Inbound/Outbound).
     */
    public static String generateNextCode(Connection conn, String tableName, String columnName,
                                           String prefix, int digits) throws SQLException {
        String sql = "SELECT " + columnName + " FROM " + tableName
                + " WHERE " + columnName + " LIKE ? ORDER BY id DESC LIMIT 1";
        int nextNumber = 1;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastKode = rs.getString(columnName);
                    String numPart = lastKode.substring(prefix.length()).replaceAll("[^0-9]", "");
                    if (!numPart.isEmpty()) {
                        nextNumber = Integer.parseInt(numPart) + 1;
                    }
                }
            }
        }
        String format = "%0" + digits + "d";
        return prefix + String.format(format, nextNumber);
    }

    public static String generateLokasiKode(String area, String rak) {
        String areaCode = "X";
        if (area != null && !area.isBlank()) {
            String trimmed = area.trim();
            String[] parts = trimmed.split("\\s+");
            areaCode = parts[parts.length - 1].toUpperCase();
        }
        String rakPadded = rak == null ? "00" : rak.trim();
        try {
            rakPadded = String.format("%02d", Integer.parseInt(rakPadded));
        } catch (NumberFormatException ignored) {
        }
        return "LOK-" + areaCode + rakPadded;
    }
}
