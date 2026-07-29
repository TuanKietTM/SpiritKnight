package com.soulknight.database;

public class PlayerSave {

    private int id;
    private String playerName;
    private int level;
    private int gold;
    private int gems;
    private double hp;
    private double energy;
    private int currentRoom;
    private int score;
    private double playerX;
    private double playerY;

    public PlayerSave() {
    }

    public PlayerSave(String playerName, int level,int gold, int gems, double hp, double energy, int currentRoom, int score) {
        this.playerName = playerName;
        this.level = level;
        this.gold = gold;
        this.gems = gems;
        this.hp = hp;
        this.energy = energy;
        this.currentRoom = currentRoom;
        this.score = score;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = gems;
    }

    public double getHp() {
        return hp;
    }

    public void setHp(double hp) {
        this.hp = hp;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public int getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(int currentRoom) {
        this.currentRoom = currentRoom;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    public double getPlayerX() {
        return playerX;
    }

    public void setPlayerX(double playerX) {
        this.playerX = playerX;
    }

    public double getPlayerY() {
        return playerY;
    }

    public void setPlayerY(double playerY) {
        this.playerY = playerY;
    }
}