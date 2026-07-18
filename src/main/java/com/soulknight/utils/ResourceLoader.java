package com.soulknight.utils;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.image.PixelWriter;

public final class ResourceLoader {

    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final Image PLACEHOLDER_IMAGE = createPlaceholderImage();

    private ResourceLoader() {
    }

    public static Image image(String resourcePath) {
        return IMAGE_CACHE.computeIfAbsent(resourcePath, ResourceLoader::loadImage);
    }

    private static Image loadImage(String resourcePath) {
        URL resource = ResourceLoader.class.getResource(resourcePath);
        return resource == null ? PLACEHOLDER_IMAGE : new Image(resource.toExternalForm(), true);
    }

    private static Image createPlaceholderImage() {
        WritableImage writableImage = new WritableImage(1, 1);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        pixelWriter.setColor(0, 0, Color.TRANSPARENT);
        return writableImage;
    }
}