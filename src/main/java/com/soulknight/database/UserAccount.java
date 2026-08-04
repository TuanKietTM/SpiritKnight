package com.soulknight.database;

public final class UserAccount {

    private final int id;
    private final String username;
    private final String passwordHash;
    private boolean firstPlay;
    private int goldBank;
    private int gemBank;

    public UserAccount(int id, String username, String passwordHash, boolean firstPlay, int goldBank, int gemBank) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstPlay = firstPlay;
        this.goldBank = Math.max(0, goldBank);
        this.gemBank = Math.max(0, gemBank);
    }

    public UserAccount(int id, String username, String passwordHash, boolean firstPlay) {
        this(id, username, passwordHash, firstPlay, 0, 0);
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isFirstPlay() { return firstPlay; }
    public void setFirstPlay(boolean firstPlay) { this.firstPlay = firstPlay; }
    public int getGoldBank() { return goldBank; }
    public void setGoldBank(int goldBank) { this.goldBank = Math.max(0, goldBank); }
    public void addGoldBank(int amount) { if (amount > 0) goldBank += amount; }
    public int getGemBank() { return gemBank; }
    public void setGemBank(int gemBank) { this.gemBank = Math.max(0, gemBank); }
    public void addGemBank(int amount) { if (amount > 0) gemBank += amount; }
}