package com.soulknight.database;

public final class UserSession {

    private static UserAccount currentUser;

    private UserSession() {
    }

    public static void login(UserAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("Account khong duoc null.");
        }

        currentUser = account;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static UserAccount getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException(
                    "Chua dang nhap."
            );
        }

        return currentUser;
    }

    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }
    public static int getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static boolean isFirstPlay() {
        return getCurrentUser().isFirstPlay();
    }

    public static void setFirstPlay(boolean firstPlay) {
        getCurrentUser().setFirstPlay(firstPlay);
    }
}