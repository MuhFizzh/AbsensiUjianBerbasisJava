package com.wms.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Utilitas baca/tulis JSON sederhana, khusus dipakai untuk kolom
 * aktivitas_kerja.detail_snapshot.
 *
 * Kenapa dibuat manual (bukan pakai library JSON seperti Gson/Jackson):
 * project ini belum punya dependency JSON apapun (lihat pom.xml), dan
 * kebutuhannya sangat sempit: menyimpan & membaca kembali daftar baris
 * flat (key/value String saja) sebagai array JSON. Menambah library
 * eksternal hanya untuk kebutuhan sekecil ini dianggap berlebihan.
 *
 * Format yang dihasilkan/dibaca:
 * [
 *   {"Barang":"Kabel HDMI 2m","Kode":"BRG-0001","Lokasi":"A-01-01","Qty":"10"},
 *   {"Barang":"Mouse Wireless","Kode":"BRG-0002","Lokasi":"A-01-02","Qty":"5"}
 * ]
 *
 * Setiap object merepresentasikan 1 baris barang dalam transaksi.
 * LinkedHashMap dipakai supaya urutan kolom (key) tetap terjaga saat
 * ditampilkan kembali sebagai tabel di UI.
 */
public final class SnapshotUtil {

    private SnapshotUtil() {
    }

    // ------------------------------------------------------------
    // Build: List<LinkedHashMap<String,String>> -> JSON String
    // ------------------------------------------------------------

    /**
     * Mengubah daftar baris (tiap baris = 1 barang, key = nama kolom,
     * value = isi kolom dalam bentuk String) menjadi teks JSON siap
     * disimpan ke kolom detail_snapshot.
     */
    public static String toJson(List<LinkedHashMap<String, String>> rows) {
        if (rows == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(objectToJson(rows.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String objectToJson(LinkedHashMap<String, String> row) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (var entry : row.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(escape(entry.getKey())).append(':').append(escape(entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) value = "";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // ------------------------------------------------------------
    // Parse: JSON String -> List<LinkedHashMap<String,String>>
    // ------------------------------------------------------------

    /**
     * Membaca kembali teks JSON hasil {@link #toJson} menjadi daftar
     * baris. Mengembalikan list kosong (bukan null) jika snapshot
     * kosong/null/tidak valid, supaya pemanggil tidak perlu null-check.
     */
    public static List<LinkedHashMap<String, String>> parse(String json) {
        List<LinkedHashMap<String, String>> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        try {
            Parser p = new Parser(json);
            p.skipWhitespace();
            if (!p.consume('[')) {
                return result;
            }
            p.skipWhitespace();
            if (p.peek() == ']') {
                return result;
            }
            while (true) {
                p.skipWhitespace();
                result.add(p.parseObject());
                p.skipWhitespace();
                if (p.peek() == ',') {
                    p.pos++;
                    continue;
                }
                break;
            }
            return result;
        } catch (Exception ex) {
            // Snapshot rusak/format tak terduga -> anggap kosong,
            // biar pemanggil bisa fallback, jangan sampai UI crash.
            return new ArrayList<>();
        }
    }

    private static class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        boolean consume(char c) {
            if (peek() == c) {
                pos++;
                return true;
            }
            return false;
        }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        LinkedHashMap<String, String> parseObject() {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            skipWhitespace();
            if (!consume('{')) return map;
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                consume(':');
                skipWhitespace();
                String value = parseString();
                map.put(key, value);
                skipWhitespace();
                if (peek() == ',') {
                    pos++;
                    continue;
                }
                break;
            }
            skipWhitespace();
            consume('}');
            return map;
        }

        String parseString() {
            StringBuilder sb = new StringBuilder();
            if (!consume('"')) return "";
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < s.length()) {
                    char next = s.charAt(pos++);
                    switch (next) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 <= s.length()) {
                                String hex = s.substring(pos, pos + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                        }
                        default -> sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
