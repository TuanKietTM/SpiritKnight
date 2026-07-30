package com.soulknight.database;

public final class UserAccount {

    private final int id;
    private final String username;
    private final String passwordHash;
    private boolean firstPlay;

    public UserAccount(int id, String username, String passwordHash, boolean firstPlay) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstPlay = firstPlay;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isFirstPlay() {
        return firstPlay;
    }

    public void setFirstPlay(boolean firstPlay) {
        this.firstPlay = firstPlay;
    }
}