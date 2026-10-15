package com.wms.util;

import com.wms.dao.AuditLogDAO;

public final class AuditLogger {

    private static final AuditLogDAO DAO =
            new AuditLogDAO();

    private AuditLogger() {
    }

    public static void log(
            String module,
            String action,
            Integer referenceId,
            String description) {

        String username =
                Session.getUsername();

        if (username == null) {
            return;
        }

        try {

            DAO.insert(
                    username,
                    module,
                    action,
                    referenceId,
                    description);

        } catch (Exception ex) {

            System.err.println(
                    "Audit log gagal: "
                    + ex.getMessage());
        }
    }
}