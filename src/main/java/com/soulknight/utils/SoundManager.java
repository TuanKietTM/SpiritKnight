package com.soulknight.utils;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
//Thi diem 1 class su dung phuong phap may huu han trang thai
public final class SoundManager {
    private static final SoundManager instance = new SoundManager();

//    Dinh nghia cac trang thai am thanh
    public enum AudioState {
        UNMUTED {
            @Override
            public AudioState nextState() {
                return MUTED;
            }

            @Override
            public void handleSFX(AudioClip clip, double volume) {
                if (clip != null) {
                    clip.play(volume);
                }
            }

            @Override
            public void applyBGMState(MediaPlayer player) {
                if (player != null) {
                    player.setMute(false);
                }
            }
        },
        MUTED {
            @Override
            public AudioState nextState() {
                return UNMUTED;
            }

            @Override
            public void handleSFX(AudioClip clip, double volume) {
//                khong phat SFX
            }

            @Override
            public void applyBGMState(MediaPlayer player) {
                if (player != null) {
                    player.setMute(true);
                }
            }
        };

//       Chuyen tiep giua cac trang thai MUTE <-> UNMUTE
        public abstract AudioState nextState();

        // Xu ly phat SFX tuy theo trang thai hoen tai
        public abstract void handleSFX(AudioClip clip, double volume);

        // Áp dụng cấu hình Mute lên MediaPlayer tùy theo trạng thái
        public abstract void applyBGMState(MediaPlayer player);
    }

    private final Map<String, AudioClip> sfxMap = new HashMap<>();
    private MediaPlayer bgmPlayer;
    private double sfxVolume = 0.3;
    private double bgmVolume = 0.2;

    // Bien luu trang thai hien tai / thay the cho cac bien booolean
    private AudioState currentState = AudioState.UNMUTED;

    private SoundManager() {
        loadSFX("Bullet", "/assets/Audio/Bullet.mp3");
        loadSFX("button", "/assets/Audio/button.mp3");
        loadSFX("switch", "/assets/Audio/Switch.mp3");
    }

    public static SoundManager getInstance() {
        return instance;
    }

    private void loadSFX(String key, String resourcePath) {
        try {
            URL res = getClass().getResource(resourcePath);
            if (res != null) {
                AudioClip clip = new AudioClip(res.toExternalForm());
                sfxMap.put(key, clip);
            } else {
                System.err.println("Khong tim thay file SFX: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[Loi tai SFX: " + resourcePath + " - " + e.getMessage());
        }
    }

    // Phat SFX bang cach uy quyen
    public void playSFX(String key) {
        AudioClip clip = sfxMap.get(key);
        currentState.handleSFX(clip, sfxVolume);
    }

    //  Phat BGM va ap dung State hien tai len MediaPlayer
    public void playBGM(String resourcePath) {
        stopBGM();
        try {
            URL res = getClass().getResource(resourcePath);
            if (res != null) {
                Media media = new Media(res.toExternalForm());
                bgmPlayer = new MediaPlayer(media);
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgmPlayer.setVolume(bgmVolume);

                // Ủy quyền cài đặt trạng thái tiếng cho State
                currentState.applyBGMState(bgmPlayer);

                bgmPlayer.play();
            } else {
                System.err.println("Khong tim thay file BGM: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("loi phat  BGM: " + e.getMessage());
        }
    }

    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
    }

    // Chuyen doi trang thai khi bam phim M
    public void toggleMute() {
        this.currentState = this.currentState.nextState();
        this.currentState.applyBGMState(this.bgmPlayer);
    }

    // Doi trang thai tim kiem neu can
    public void setState(AudioState newState) {
        if (newState != null && this.currentState != newState) {
            this.currentState = newState;
            this.currentState.applyBGMState(this.bgmPlayer);
        }
    }

    public AudioState getCurrentState() {
        return this.currentState;
    }

    public boolean isMuted() {
        return this.currentState == AudioState.MUTED;
    }

    public void setBGMVolume(double volume) {
        this.bgmVolume = volume;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(volume);
        }
    }

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