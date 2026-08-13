package com.soulknight.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * SoundManager - Quản lý âm thanh bằng máy hữu hạn trạng thái.
 */
public final class SoundManager {
    private static final SoundManager instance = new SoundManager();
    private final Map<String, AudioClip> sfxMap = new HashMap<>();
    private MediaPlayer bgmPlayer;
    private String currentBgmPath = "";
    private double sfxVolume = 0.3;
    private double bgmVolume = 0.2;
    private Timeline bgmFadeTimeline;
    private AudioState currentState = AudioState.UNMUTED;
    private SoundManager() {
        // Tải các SFX sử dụng trong game
        loadSFX("Bullet", "/assets/Audio/Bullet.mp3");
        loadSFX("button", "/assets/Audio/button.mp3");
        loadSFX("switch", "/assets/Audio/Switch.mp3");
        loadSFX("Typing", "/assets/Audio/keyboard.mp3");
        loadSFX("BIOS", "/assets/Audio/computer fan ambience.mp3");
        loadSFX("BUG", "/assets/Audio/warning.mp3");
        loadSFX("Radio", "/assets/Audio/radio_static.mp3");
        loadSFX("Portal", "/assets/Audio/portal.mp3");
        loadSFX("Sword", "/assets/Audio/sword.mp3");
        loadSFX("bird_sound", "/assets/Audio/bird_sound.mp3");
        loadSFX("cat_sound", "/assets/Audio/cat_sound.mp3");
        loadSFX("wolf_sound", "/assets/Audio/wolf_sound.mp3");
        loadSFX("ghost_sound", "/assets/Audio/ghost_sound.mp3");
        loadSFX("Pistol_Fire", "/assets/Audio/pistol.mp3");
        loadSFX("SMG_Fire", "/assets/Audio/smg.mp3");
        loadSFX("Sword_Swing", "/assets/Audio/sword_1.mp3");
        loadSFX("Fish_Slap", "/assets/Audio/fish.mp3");
        loadSFX("Blaster_Fire", "/assets/Audio/blaster.mp3");
        loadSFX("Shotgun_Fire", "/assets/Audio/short_gun.mp3");
        loadSFX("Sniper_Fire", "/assets/Audio/sniper.mp3");
        loadSFX("Magic_Cast", "/assets/Audio/magic.mp3");
        loadSFX("attack_box", "/assets/Audio/attack_box.mp3");
        loadSFX("laser_gun", "/assets/Audio/laser_gun.mp3");
        loadSFX("chain_lighting", "/assets/Audio/chain_lighting.mp3");
        loadSFX("death_explosion", "/assets/Audio/explosion.mp3");
        loadSFX("dragon_breath", "/assets/Audio/dragon_breath.mp3");
        loadSFX("dragon_explosion", "/assets/Audio/dragon_explosion.mp3");
        loadSFX("holy_nova", "/assets/Audio/holy_nova.mp3");
        loadSFX("railgun_fire", "/assets/Audio/railgun_fire.mp3");
        loadSFX("ion_gun", "/assets/Audio/blaster.mp3");
        loadSFX("ion_explosion", "/assets/Audio/ion_explosion.mp3");
        loadSFX("portal_open", "/assets/Audio/portal_open.mp3");
        loadSFX("heal", "/assets/Audio/heal.mp3");
        loadSFX("boss_die","/assets/Audio/die.mp3");
        loadSFX("boss_roar","/assets/Audio/roam.mp3");
        loadSFX("boss_step","/assets/Audio/step.mp3");
        loadSFX("boss_stomp","/assets/Audio/shockwave'.mp3");
        loadSFX("boss_shoot_laser","/assets/Audio/ion_boss.mp3");
        loadSFX("boss_shotgun","assets/Audio/s_gun.mp3");
    }

    public static SoundManager getInstance() {
        return instance;
    }

    public void loadSFX(String key, String resourcePath) {
        try {
            URL res = getClass().getResource(resourcePath);
            if (res != null) {
                AudioClip clip = new AudioClip(res.toExternalForm());
                sfxMap.put(key, clip);
            } else {
                System.err.println("Không thấy SFX tại đường dẫn: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("Lỗi nạp SFX (" + resourcePath + "): " + e.getMessage());
        }
    }

    public void playSFX(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) return;

        AudioClip clip = sfxMap.get(keyOrPath);
        if (clip == null && keyOrPath.contains("/")) {
            try {
                URL res = getClass().getResource(keyOrPath);
                if (res != null) {
                    clip = new AudioClip(res.toExternalForm());
                    sfxMap.put(keyOrPath, clip);
                }
            } catch (Exception e) {
                System.err.println("Lỗi nạp trực tiếp SFX: " + keyOrPath + " - " + e.getMessage());
            }
        }

        if (clip != null) {
            clip.setCycleCount(1);
            currentState.handleSFX(clip, sfxVolume);
        } else {
            System.err.println("Không thể phát SFX: " + keyOrPath);
        }
    }

    public void playSFXShort(String keyOrPath, double durationSeconds) {
        if (keyOrPath == null || keyOrPath.isBlank() || isMuted()) return;

        AudioClip clip = sfxMap.get(keyOrPath);
        if (clip == null && keyOrPath.contains("/")) {
            try {
                URL res = getClass().getResource(keyOrPath);
                if (res != null) {
                    clip = new AudioClip(res.toExternalForm());
                    sfxMap.put(keyOrPath, clip);
                }
            } catch (Exception ignored) {
            }
        }

        if (clip != null) {
            clip.setCycleCount(1);
            currentState.handleSFX(clip, sfxVolume);

            final AudioClip finalClip = clip;
            Timeline stopTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(durationSeconds),
                            event -> finalClip.stop())
            );
            stopTimeline.play();
        } else {
            System.err.println("Không thể phát SFX: " + keyOrPath);
        }
    }

    public void playLoopSFX(String key) {
        if (isMuted()) return;
        AudioClip clip = sfxMap.get(key);
        if (clip != null && !clip.isPlaying()) {
            clip.setCycleCount(AudioClip.INDEFINITE);
            clip.setVolume(sfxVolume);
            clip.play();
        }
    }

    public void stopSFX(String key) {
        AudioClip clip = sfxMap.get(key);
        if (clip != null) {
            clip.stop();
        }
    }

    public void stopAllSFX() {
        for (AudioClip clip : sfxMap.values()) {
            if (clip != null) {
                clip.stop();
            }
        }
    }

    public void playBGM(String resourcePath) {
        playBGM(resourcePath, 0.5);
    }

    public void playBGM(String resourcePath, double fadeDurationSeconds) {
        if (resourcePath != null && resourcePath.equals(currentBgmPath) && bgmPlayer != null) {
            return;
        }

        if (bgmPlayer != null && bgmPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            double halfDuration = fadeDurationSeconds / 2.0;
            fadeOutBGM(halfDuration, () -> fadeInNewBGM(resourcePath, halfDuration));
        } else {
            fadeInNewBGM(resourcePath, fadeDurationSeconds);
        }
    }

    private void fadeInNewBGM(String resourcePath, double durationSeconds) {
        stopFadeTimeline();
        stopBGM();

        try {
            URL res = getClass().getResource(resourcePath);
            if (res != null) {
                Media media = new Media(res.toExternalForm());
                bgmPlayer = new MediaPlayer(media);
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                currentState.applyBGMState(bgmPlayer);

                if (isMuted()) {
                    bgmPlayer.setVolume(0);
                } else {
                    bgmPlayer.setVolume(0);
                    bgmFadeTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(durationSeconds),
                                    new KeyValue(bgmPlayer.volumeProperty(), bgmVolume))
                    );
                    bgmFadeTimeline.play();
                }

                bgmPlayer.play();
                currentBgmPath = resourcePath;
            } else {
                System.err.println("Không thấy BGM: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("Lỗi BGM: " + e.getMessage());
        }
    }

    public void fadeOutBGM(double durationSeconds, Runnable onFinished) {
        if (bgmPlayer == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        stopFadeTimeline();

        bgmFadeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(durationSeconds),
                        new KeyValue(bgmPlayer.volumeProperty(), 0))
        );
        bgmFadeTimeline.setOnFinished(e -> {
            stopBGM();
            if (onFinished != null) onFinished.run();
        });
        bgmFadeTimeline.play();
    }

    public void stopBGM() {
        stopFadeTimeline();
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
            currentBgmPath = "";
        }
    }

    private void stopFadeTimeline() {
        if (bgmFadeTimeline != null) {
            bgmFadeTimeline.stop();
            bgmFadeTimeline = null;
        }
    }

    public void stopAll() {
        stopBGM();
        stopAllSFX();
    }

    public void toggleMute() {
        this.currentState = this.currentState.nextState();
        if (this.bgmPlayer != null) {
            this.currentState.applyBGMState(this.bgmPlayer);
            if (isMuted()) {
                this.bgmPlayer.setVolume(0);
            } else {
                this.bgmPlayer.setVolume(this.bgmVolume);
            }
        }
    }

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
        this.bgmVolume = Math.max(0.0, Math.min(1.0, volume));
        if (this.bgmVolume == 0.0) {
            stopFadeTimeline();
        }

        if (bgmPlayer != null) {
            bgmPlayer.setVolume(this.bgmVolume);
            if (this.bgmVolume == 0.0) {
                bgmPlayer.setMute(true);
            } else if (!isMuted()) {
                bgmPlayer.setMute(false);
            }
        }
    }

    public void setSFXVolume(double volume) {
        this.sfxVolume = Math.max(0.0, Math.min(1.0, volume));
        if (this.sfxVolume == 0.0) {
            stopAllSFX();
        }
    }

    public double getBgmVolume() {
        return this.bgmVolume;
    }

    public double getSfxVolume() {
        return this.sfxVolume;
    }

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
                // Không làm gì khi muted
            }

            @Override
            public void applyBGMState(MediaPlayer player) {
                if (player != null) {
                    player.setMute(true);
                }
            }
        };

        public abstract AudioState nextState();

        public abstract void handleSFX(AudioClip clip, double volume);

        public abstract void applyBGMState(MediaPlayer player);
    }
}