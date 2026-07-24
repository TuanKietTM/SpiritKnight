package com.soulknight.model;

import java.util.Objects;

public record StoryFrame(String imagePath, String title, String text, double displaySeconds) {

    public StoryFrame {
        Objects.requireNonNull(imagePath, "imagePath khong null");
        Objects.requireNonNull(title, "title khong null");
        Objects.requireNonNull(text, "text khong  null");

        if (displaySeconds < 0) {
            throw new IllegalArgumentException("displaySeconds khong am ");
        }
    }
}