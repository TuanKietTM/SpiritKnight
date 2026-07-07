package com.soulknight.utils;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.image.PixelWriter;

public final class ResourceLoader {

    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AudioClip> AUDIO_CACHE = new ConcurrentHashMap<>();
    private static final Image PLACEHOLDER_IMAGE = createPlaceholderImage();

    private ResourceLoader() {
    }

    public static Image image(String resourcePath) {
        return IMAGE_CACHE.computeIfAbsent(resourcePath, ResourceLoader::loadImage);
    }

    public static AudioClip audioClip(String resourcePath) {
        AudioClip audioClip = AUDIO_CACHE.get(resourcePath);
        if (audioClip != null) {
            return audioClip;
        }

        AudioClip loaded = loadAudioClip(resourcePath);
        if (loaded != null) {
            AUDIO_CACHE.put(resourcePath, loaded);
        }
        return loaded;
    }

    public static MediaPlayer mediaPlayer(String resourcePath) {
        URL resource = ResourceLoader.class.getResource(resourcePath);
        if (resource == null) {
            return null;
        }
        return new MediaPlayer(new Media(resource.toExternalForm()));
    }

    private static Image loadImage(String resourcePath) {
        URL resource = ResourceLoader.class.getResource(resourcePath);
        return resource == null ? PLACEHOLDER_IMAGE : new Image(resource.toExternalForm(), true);
    }

    private static AudioClip loadAudioClip(String resourcePath) {
        URL resource = ResourceLoader.class.getResource(resourcePath);
        return resource == null ? null : new AudioClip(resource.toExternalForm());
    }

    private static Image createPlaceholderImage() {
        WritableImage writableImage = new WritableImage(1, 1);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        pixelWriter.setColor(0, 0, Color.TRANSPARENT);
        return writableImage;
    }
}
