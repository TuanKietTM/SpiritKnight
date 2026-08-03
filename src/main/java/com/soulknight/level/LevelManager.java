package com.soulknight.level;

import com.soulknight.entity.EnemyArchetype;
import com.soulknight.mission.BossDefeatMission;
import com.soulknight.mission.CollectItemMission;
import com.soulknight.mission.KillTargetMission;
import com.soulknight.mission.Mission;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Weapon;
import java.util.List;

public final class LevelManager {

    // Đếm số lần mở hộp phần thưởng để trả về vũ khí theo thứ tự
    private int rewardBoxCount = 0;

    private final List<LevelDefinition> levels = List.of(
            new LevelDefinition(
                    1,
                    "The Training Dungeon",
                    "EASY",
                    20,
                    4,
                    0,
                    5,
                    0,
                    false,
                    new Gun("Training Gun", 10, 0.25, 560.0, 0.0)
                            .withImage("/assets/WeaponImage/GunImage/OldPistol.png"),
                    () -> new KillTargetMission(
                            "The Training Dungeon",
                            "Defeat exactly 5 basic Slimes",
                            5,
                            EnemyArchetype.MELEE_NORMAL)),
            new LevelDefinition(
                    2,
                    "The Haunted Corridors",
                    "MEDIUM",
                    24,
                    5,
                    8,
                    7,
                    3,
                    false,
                    null,
                    () -> new CollectItemMission(
                            "The Haunted Corridors",
                            "Collect 3 Energy Crystals",
                            3)),
            new LevelDefinition(
                    3,
                    "The Knight's Tomb",
                    "HARD",
                    32,
                    7,
                    15,
                    9,
                    0,
                    true,
                    null,
                    () -> new BossDefeatMission(
                            "The Knight's Tomb",
                            "Eliminate the Grand Knight")))
    ;

    private int currentLevelIndex = 1;

    public void startNewRun() {
        currentLevelIndex = 1;
        rewardBoxCount = 0; // Reset số lần mở hộp khi bắt đầu run mới
    }

    public LevelDefinition getCurrentLevel() {
        return levels.get(currentLevelIndex - 1);
    }

    public boolean advanceLevel() {
        if (currentLevelIndex < levels.size()) {
            currentLevelIndex++;
            return true;
        }
        return false;
    }

    public boolean isFinalLevel() {
        return currentLevelIndex == levels.size();
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public int scaleEnemyHealth(int baseHealth) {
        return Math.max(1, (int) Math.round(baseHealth * (1.0 + 0.3 * (currentLevelIndex - 1))));
    }

    public int scaleEnemyDamage(int baseDamage) {
        return Math.max(1, baseDamage + (currentLevelIndex - 1));
    }

    public int scaleEnemyCount(int baseCount) {
        return Math.max(1, baseCount + 2 * (currentLevelIndex - 1));
    }

    public double scaleEnemySpeed(double baseSpeed) {
        return baseSpeed * (1.0 + 0.15 * (currentLevelIndex - 1));
    }

    public Weapon getLevelRewardWeapon() {
        return getCurrentLevel().rewardWeapon();
    }

    /**
     * Lấy vũ khí làm phần thưởng khi hoàn thành nhiệm vụ của màn hiện tại.
     * Thứ tự vũ khí từ hộp quà đầu tiên đến cuối cùng:
     * 1. Shotgun (Shotgun.png)
     * 2. Sniper (Sniper.png)
     * 3. SMG (SMG.png)
     * 4. Laser Rifle (sunglaser.png)
     * 5+ Laser Rifle (sunglaser.png)
     * 
     * Lưu ý: Mặc định ban đầu người chơi đã có Old Pistol và Old Sword,
     * nên hộp quà đầu tiên là Shotgun (không trùng Old Pistol).
     */
    public Weapon getRewardWeaponForCurrentLevel() {
        rewardBoxCount++;
        return switch (rewardBoxCount) {
            case 1 -> com.soulknight.weapon.WeaponType.SHOTGUN.createWeapon();
            case 2 -> com.soulknight.weapon.WeaponType.SNIPER.createWeapon();
            case 3 -> com.soulknight.weapon.WeaponType.SMG.createWeapon();
            case 4 -> com.soulknight.weapon.WeaponType.LASER_RIFLE.createWeapon();
            default -> com.soulknight.weapon.WeaponType.LASER_RIFLE.createWeapon();
        };
    }

    public Mission createMissionForCurrentLevel() {
        return getCurrentLevel().missionFactory().get();
    }

    public String getLevelBanner() {
        LevelDefinition level = getCurrentLevel();
        return "Level " + level.number() + " - " + level.name() + " (" + level.difficulty() + ")";
    }

    public String getScaledSummary() {
        LevelDefinition level = getCurrentLevel();
        return "HP x" + formatMultiplier(1.0 + 0.3 * (currentLevelIndex - 1))
                + ", DMG +" + (currentLevelIndex - 1)
                + ", Count +" + (2 * (currentLevelIndex - 1));
    }

    private String formatMultiplier(double value) {
        return String.format("%.1f", value);
    }

    public record LevelDefinition(
            int number,
            String name,
            String difficulty,
            int baseEnemyHealth,
            int baseEnemyDamage,
            int spawnIntervalSeconds,
            int baseEnemyCount,
            int bossSpawnDelaySeconds,
            boolean bossLevel,
            Weapon rewardWeapon,
            java.util.function.Supplier<Mission> missionFactory) {
    }
}
