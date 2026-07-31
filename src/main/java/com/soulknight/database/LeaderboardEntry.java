package com.soulknight.database;

public record LeaderboardEntry(
        int rank,
        String playerName,
        int score,
        int gold
) {}