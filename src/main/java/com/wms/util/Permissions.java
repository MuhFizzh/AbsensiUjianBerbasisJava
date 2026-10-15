package com.wms.util;

public final class Permissions {

    private Permissions() {
    }

    public static boolean canAccess(
            String menu) {

        String role =
                Session.getRole();

        if (role == null) {
            return false;
        }

        if ("Administrator".equals(role)) {
            return true;
        }

        return switch (menu) {

            case "Dashboard",
                 "Inbound",
                 "Outbound",
                 "Stock",
                 "Stock Opname",
                 "Create Request" ->

                    role.equals(
                            "Warehouse Operator")
                    || role.equals(
                            "Staff Gudang");

            case "Approval" ->
                    role.equals(
                            "Warehouse Operator");

            case "Master Data" ->
                    role.equals(
                            "Administrator");

            case "Aktivitas Kerja" ->
                    role.equals(
                            "Administrator");

            default -> false;
        };
    }

    public static boolean canApprove() {

        return "Administrator".equals(
                Session.getRole())

                || "Warehouse Operator".equals(
                Session.getRole());
    }

    public static boolean canDoStockOpname() {

        return "Administrator".equals(
                Session.getRole())

                || "Warehouse Operator".equals(
                Session.getRole());
    }

    public static boolean canVoidTransaction() {

        return "Administrator".equals(
                Session.getRole())

                || "Warehouse Operator".equals(
                Session.getRole());
    }

    /**
     * Konfirmasi transaksi (Terima Inbound / Kirim Outbound / Proses
     * Stock Opname) khusus Administrator - staf lain hanya bisa
     * membuat draft-nya, validasi akhir tetap di admin (maker-checker).
     */
    public static boolean canConfirmTransaction() {

        return "Administrator".equals(
                Session.getRole());
    }
}