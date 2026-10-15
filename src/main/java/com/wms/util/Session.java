package com.wms.util;

import com.wms.model.User;

public final class Session {

    private static User user;

    private Session() {
    }

    public static void login(User authenticatedUser) {
        user = authenticatedUser;
    }

    public static User getUser() {
        return user;
    }

    public static String getUsername() {
        return user == null ? null : user.getUsername();
    }

    public static String getDisplayName() {
        return user == null ? null : user.getNama();
    }

    public static String getRole() {
        return user == null ? null : user.getRole();
    }

    public static boolean isLoggedIn() {
        return user != null;
    }

    public static void logout() {
        user = null;
    }
}