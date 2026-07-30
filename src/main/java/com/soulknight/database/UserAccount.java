package com.soulknight.database;

public final class UserAccount {

    private final int id;
    private final String username;
    private final String passwordHash;

    public UserAccount(
            int id,
            String username,
            String passwordHash
    ) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
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
}