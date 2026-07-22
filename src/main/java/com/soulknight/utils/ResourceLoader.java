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

    public static Image loadImage(String resourcePath) {
        URL resource = ResourceLoader.class.getResource(resourcePath);
        // Tai anh dong bo (khong dung background loading) de getWidth()/getHeight()
        // co gia tri ngay lap tuc; tranh bi ket width=0 khi chay dang module (JPMS)
        return resource == null ? PLACEHOLDER_IMAGE : new Image(resource.toExternalForm());
    }

    private static Image createPlaceholderImage() {
        WritableImage writableImage = new WritableImage(1, 1);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        pixelWriter.setColor(0, 0, Color.TRANSPARENT);
        return writableImage;
    }
}