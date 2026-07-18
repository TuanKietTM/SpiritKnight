package com.soulknight.utils;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class SoundManager {
    private static final SoundManager instance = new SoundManager();

    private final Map<String, AudioClip> sfxMap = new HashMap<>();
    private MediaPlayer bgmPlayer;
    private double sfxVolume = 0.8;
    private double bgmVolume = 0.5;

    private SoundManager() {
        // Tự động tải trước (preload) các hiệu ứng âm thanh ngắn để tránh bị giật lag khi gọi lần đầu
        loadSFX("Bullet", "/assets/Audio/Bullet.mp3");
        loadSFX("button", "/assets/Audio/button.mp3");
        loadSFX("switch", "/assets/Audio/Switch.mp3");
    }

    public static SoundManager getInstance() {
        return instance;
    }

    // Tải file SFX
    private void loadSFX(String key, String resourcePath) {
        try {
            URL res = getClass().getResource(resourcePath);
            if (res != null) {
                AudioClip clip = new AudioClip(res.toExternalForm());
                sfxMap.put(key, clip);
            } else {
                System.err.println("[SOUND ERROR] Không tìm thấy file SFX: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[SOUND ERROR] Lỗi tải SFX: " + resourcePath + " - " + e.getMessage());
        }
    }

    // Phát hiệu ứng âm thanh (SFX) ngắn
    public void playSFX(String key) {
        AudioClip clip = sfxMap.get(key);
        if (clip != null) {
            clip.play(sfxVolume);
        }
    }

    // Phát nhạc nền (BGM) có vòng lặp
    public void playBGM(String resourcePath) {
        stopBGM(); // Dừng nhạc nền cũ nếu đang phát
        try {
            URL res = getClass().getResource(resourcePath);
            if (res != null) {
                Media media = new Media(res.toExternalForm());
                bgmPlayer = new MediaPlayer(media);
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Vòng lặp vô hạn
                bgmPlayer.setVolume(bgmVolume);
                bgmPlayer.play();
            } else {
                System.err.println("[SOUND ERROR] Không tìm thấy file BGM: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[SOUND ERROR] Lỗi phát BGM: " + e.getMessage());
        }
    }

    // Dừng nhạc nền
    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
    }
    public void setBGMVolume(double volume) {
        this.bgmVolume = volume;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(volume);
        }
    }

    // Thay đổi âm lượng Hiệu ứng (Giá trị truyền vào từ 0.0 đến 1.0)
    public void setSFXVolume(double volume) {
        this.sfxVolume = volume;
    }

    public double getBgmVolume() {
        return this.bgmVolume;
    }
    public double getSfxVolume() {
        return this.sfxVolume;
    }
}