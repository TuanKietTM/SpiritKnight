package com.soulknight.ui;

import com.soulknight.engine.GameWorld;
import com.soulknight.pet.PetSelectionManager;
import com.soulknight.pet.PetType;
import com.soulknight.utils.SoundManager;
import com.soulknight.weapon.WeaponSelectionManager;
import com.soulknight.weapon.WeaponType;
import com.soulknight.entity.HeroSelectionManager;
import com.soulknight.entity.HeroType;
import com.soulknight.database.ShopDAO;
import com.soulknight.database.UserSession;
import com.soulknight.utils.DatabaseExecutor;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ShopController {

    @FXML private Button closeButton;
    @FXML private Label coinLabel;
    @FXML private Label gemLabel;

    @FXML private Button tabPet;
    @FXML private Button tabWeapon;
    @FXML private Button tabHero;
    @FXML private Button tabUpgrade;

    @FXML private FlowPane itemGrid;
    @FXML private Label selectedItemName;
    @FXML private Label selectedItemDesc;
    @FXML private Button actionButton;

    private Runnable onCloseCallback;
    private GameWorld gameWorld;
    private PetType selectedPet = null;
    private WeaponType selectedWeapon = null;
    private AnimationTimer shopAnimTimer;
    private double elapsedTime = 0;
    private long lastTime = 0;
    private static final double SHOP_CANVAS_SIZE = 80.0;
    private static final double SHOP_PET_SIZE = 64.0;
    private static final String ITEM_TYPE_PET = "PET";
    private static final String ITEM_TYPE_WEAPON = "WEAPON";
    private static final String ITEM_TYPE_HERO = "HERO";

    private static final PetType STARTER_PET = PetType.CAT;
    private static final WeaponType STARTER_WEAPON = WeaponType.BLASTER;
    private static final HeroType STARTER_HERO = HeroType.KNIGHT;

    private final ShopDAO shopDAO = new ShopDAO();

    private final Set<PetType> ownedPets = new HashSet<>();
    private final Set<WeaponType> ownedWeapons = new HashSet<>();
    private final Set<HeroType> ownedHeroes = new HashSet<>();

    private int currentGold;
    private int currentGems;
    private boolean shopLoading;
    private boolean shopDataLoaded = false;
    private int loadedUserId = -1;

    // Luu anh da tai de khong doc lai file moi lan mo shop
    private final Map<String, Image> imageCache = new HashMap<>();
    private final List<PetCanvasRenderer> activeRenderers = new ArrayList<>();
    private final List<HeroCanvasRenderer> activeHeroRenderers = new ArrayList<>();

    private record PetCanvasRenderer(PetType pet, Canvas canvas, Image idleSheet) {}
    private record HeroCanvasRenderer(HeroType hero, Canvas canvas, Image idleSheet) {
    }

    private java.util.function.Consumer<String> onShowLoading;
    private Runnable onHideLoading;

    public void setLoadingCallbacks(java.util.function.Consumer<String> onShowLoading, Runnable onHideLoading) {
        this.onShowLoading = onShowLoading;
        this.onHideLoading = onHideLoading;
    }
    private void showLoadingOverlay(String message) {
        if (onShowLoading != null) {
            onShowLoading.accept(message);
        }
    }

    private void hideLoadingOverlay() {
        if (onHideLoading != null) {
            onHideLoading.run();
        }
    }

    @FXML
    public void initialize() {
        tabPet.setOnAction(e -> switchTab(tabPet, this::loadPetShop));
        tabWeapon.setOnAction(e -> switchTab(tabWeapon, this::loadWeaponShop));
        tabHero.setOnAction(e -> switchTab(tabHero, this::loadHeroShop));
        tabUpgrade.setOnAction(e -> switchTab(tabUpgrade, () -> loadPlaceholderCategory("UPGRADE", "UP")));

        closeButton.setOnAction(e -> {
            SoundManager.getInstance().playSFX("button");
            stopAnimation();
            if (onCloseCallback != null) onCloseCallback.run();
        });

        setupAnimationLoop();
    }

    public void setup(GameWorld world, Runnable onClose) {
        this.gameWorld = world;
        this.onCloseCallback = onClose;
        selectedPet = null;
        selectedWeapon = null;
        actionButton.setVisible(false);
        startAnimation();

        int currentUserId = UserSession.getCurrentUserId();

        // Mo lai shop thi hien du lieu trong RAM ngay, sau do cap nhat vang ngam
        if (shopDataLoaded && loadedUserId == currentUserId) {
            setShopLoading(false);
            updateCurrencyLabels();
            showTab(tabPet, this::loadPetShop);
            refreshCurrencies();
            return;
        }

        setShopLoading(true);
        loadShopData();
    }
    private void loadShopData() {
        int userId = UserSession.getCurrentUserId();

        if (onShowLoading != null) {
            onShowLoading.accept("Loading shop...");
        }
        // Chi cap do khoi dau truoc, cac du lieu con lai tai song song de giam thoi gian cho
        CompletableFuture
                .runAsync(() -> shopDAO.grantStarterItems(userId, STARTER_PET.name(), STARTER_WEAPON.name()),
                        DatabaseExecutor.getExecutor())
                .thenCompose(ignored -> {
                    CompletableFuture<Set<String>> petsFuture = CompletableFuture.supplyAsync(
                            () -> shopDAO.getOwnedItems(userId, ITEM_TYPE_PET),DatabaseExecutor.getExecutor()
                    );
                    CompletableFuture<Set<String>> weaponsFuture = CompletableFuture.supplyAsync(
                            () -> shopDAO.getOwnedItems(userId, ITEM_TYPE_WEAPON),DatabaseExecutor.getExecutor()
                    );
                    CompletableFuture<Integer> goldFuture = CompletableFuture.supplyAsync(
                            () -> shopDAO.getGold(userId), DatabaseExecutor.getExecutor()
                    );
                    CompletableFuture<Integer> gemsFuture = CompletableFuture.supplyAsync(
                            () -> shopDAO.getGems(userId), DatabaseExecutor.getExecutor()
                    );
                    CompletableFuture<Set<String>> heroesFuture = CompletableFuture.supplyAsync(() ->
                            shopDAO.getOwnedItems(userId, ITEM_TYPE_HERO),DatabaseExecutor.getExecutor()
                    );

                    return CompletableFuture.allOf(petsFuture, weaponsFuture, heroesFuture, goldFuture, gemsFuture)
                            .thenApply(value -> new ShopData(
                                    petsFuture.join(),
                                    weaponsFuture.join(),
                                    heroesFuture.join(),
                                    goldFuture.join(),
                                    gemsFuture.join()
                            ));
                })
                .thenAccept(data -> Platform.runLater(() -> {
                    hideLoadingOverlay();
                    applyShopData(userId, data);
                }))
                .exceptionally(exception -> {
                    exception.printStackTrace();

                    Platform.runLater(() -> {
                        hideLoadingOverlay();
                        showDatabaseError();
                    });

                    return null;
                });
    }

    private void applyShopData(int userId, ShopData data) {
        ownedPets.clear();
        ownedWeapons.clear();

        addOwnedPets(data.petCodes());
        addOwnedWeapons(data.weaponCodes());
        addOwnedHeroes(data.heroCodes());

        currentGold = data.gold();
        currentGems = data.gems();
        loadedUserId = userId;
        shopDataLoaded = true;

        updateCurrencyLabels();
        setShopLoading(false);
        showTab(tabPet, this::loadPetShop);
    }

    private void addOwnedPets(Set<String> petCodes) {
        for (String code : petCodes) {
            try {
                ownedPets.add(PetType.valueOf(code));
            } catch (IllegalArgumentException ignored) {
                System.err.println("Pet trong DB khong ton tai trong enum: " + code);
            }
        }
    }

    private void addOwnedWeapons(Set<String> weaponCodes) {
        for (String code : weaponCodes) {
            try {
                ownedWeapons.add(WeaponType.valueOf(code));
            } catch (IllegalArgumentException ignored) {
                System.err.println("Weapon trong DB khong ton tai trong enum: " + code);
            }
        }
    }

    // Chi tai lai tien tich luy khi mo lai shop, khong tai lai toan bo inventory
    private void refreshCurrencies() {
        int userId = UserSession.getCurrentUserId();

        CompletableFuture<Integer> goldFuture = CompletableFuture.supplyAsync(
                () -> shopDAO.getGold(userId),
                DatabaseExecutor.getExecutor()
        );

        CompletableFuture<Integer> gemsFuture = CompletableFuture.supplyAsync(
                () -> shopDAO.getGems(userId),
                DatabaseExecutor.getExecutor()
        );

        CompletableFuture
                .allOf(goldFuture, gemsFuture)
                .thenRun(() -> Platform.runLater(() -> {
                    currentGold = goldFuture.join();
                    currentGems = gemsFuture.join();
                    updateCurrencyLabels();
                }))
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    return null;
                });
    }

    private void showDatabaseError() {
        setShopLoading(false);
        selectedItemName.setText("DATABASE ERROR");
        selectedItemDesc.setText("Khong the tai du lieu cua hang.");
        actionButton.setVisible(false);
    }

    private void setShopLoading(boolean loading) {
        shopLoading = loading;

        tabPet.setDisable(loading);
        tabWeapon.setDisable(loading);
        tabHero.setDisable(loading);
        tabUpgrade.setDisable(loading);
        actionButton.setDisable(loading);

        if (loading) {
            coinLabel.setText("Loading...");
            gemLabel.setText("Loading...");
        }
    }

    private void updateCurrencyLabels() {
        coinLabel.setText(String.valueOf(currentGold));
        gemLabel.setText(String.valueOf(currentGems));
    }

    private void switchTab(Button selectedTab, Runnable loadContent) {
        if (shopLoading) return;
        SoundManager.getInstance().playSFX("button");
        showTab(selectedTab, loadContent);
    }

    // Dung khi code tu mo tab de khong phat am thanh nut
    private void showTab(Button selectedTab, Runnable loadContent) {
        tabPet.getStyleClass().remove("tab-active");
        tabWeapon.getStyleClass().remove("tab-active");
        tabHero.getStyleClass().remove("tab-active");
        tabUpgrade.getStyleClass().remove("tab-active");

        selectedTab.getStyleClass().add("tab-active");
        selectedPet = null;
        selectedWeapon = null;
        actionButton.setVisible(false);
        loadContent.run();
    }

    private void loadPetShop() {
        activeHeroRenderers.clear();
        activeRenderers.clear();
        itemGrid.getChildren().clear();

        PetType equippedPet = PetSelectionManager.getInstance().getSelectedPet();

        for (PetType pet : PetType.values()) {
            if (!pet.hasPet()) continue;

            VBox card = createPetCard(pet, equippedPet);
            itemGrid.getChildren().add(card);
        }
    }

    private VBox createPetCard(PetType pet, PetType equippedPet) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("item-card");

        boolean isOwned = ownedPets.contains(pet);
        boolean isEquipped = (pet == equippedPet);

        if (isEquipped) {
            card.getStyleClass().add("item-card-equipped");
        }

        if (!isOwned) {
            card.getStyleClass().add("item-card-locked");
        }

        Canvas canvas = new Canvas(SHOP_CANVAS_SIZE, SHOP_CANVAS_SIZE);
        Image idleSheet = loadImage(pet.getIdleImagePath());

        if (idleSheet != null) {
            activeRenderers.add(new PetCanvasRenderer(pet, canvas, idleSheet));
        }

        Label nameLabel = new Label(pet.getDisplayName());
        nameLabel.getStyleClass().add("item-card-title");

        card.getChildren().addAll(canvas, nameLabel);

        card.setOnMouseClicked(e -> {
            if (shopLoading) {
                return;
            }

            playPetSound(pet);

            itemGrid.getChildren().forEach(
                    node -> node.getStyleClass().remove("item-card-selected")
            );
            card.getStyleClass().add("item-card-selected");

            this.selectedPet = pet;
            this.selectedWeapon = null;

            selectedItemName.setText(pet.getDisplayName());

            boolean isCurrentOwned = ownedPets.contains(pet);
            boolean isCurrentEquipped =
                    PetSelectionManager.getInstance().isSelected(pet);

            String description =
                    "PACE: " + pet.getMoveSpeed() + " | Supporter";

            if (!isCurrentOwned) {
                description += " | PRICE: " + pet.getPrice() + " GOLD";
            }

            selectedItemDesc.setText(description);
            actionButton.setVisible(true);

            if (isCurrentEquipped) {
                actionButton.setText("EQUIPPED");
                actionButton.setDisable(true);
                actionButton.setOnAction(null);

            } else if (isCurrentOwned) {
                actionButton.setText("EQUIP PET");
                actionButton.setDisable(false);
                actionButton.setOnAction(evt -> {
                    playPetSound(pet);
                    equipPet(pet);
                });

            } else {
                actionButton.setText("BUY - " + pet.getPrice() + " GOLD");
                actionButton.setDisable(false);
                actionButton.setOnAction(evt -> {
                    SoundManager.getInstance().playSFX("button");
                    purchasePet(pet);
                });
            }
        });

        return card;
    }

    private void purchasePet(PetType pet) {
        if (shopLoading || ownedPets.contains(pet)) {
            return;
        }

        int price = pet.getPrice();
        setShopLoading(true);
        showLoadingOverlay("Buying pet...");

        CompletableFuture
                .supplyAsync(() ->
                        shopDAO.purchaseItem(UserSession.getCurrentUserId(), UserSession.getCurrentUser().getUsername(),
                                ITEM_TYPE_PET, pet.name(), price),DatabaseExecutor.getExecutor()
                )
                .thenAccept(result -> Platform.runLater(() -> {
                    hideLoadingOverlay();
                    setShopLoading(false);

                    switch (result) {
                        case SUCCESS -> {
                            ownedPets.add(pet);
                            currentGold = Math.max(0, currentGold - price);
                            updateCurrencyLabels();

                            selectedItemName.setText(pet.getDisplayName());
                            selectedItemDesc.setText("PURCHASE SUCCESS");

                            loadPetShop();
                        }

                        case ALREADY_OWNED -> {
                            ownedPets.add(pet);
                            updateCurrencyLabels();
                            selectedItemDesc.setText("You already own this pet.");
                            loadPetShop();
                        }

                        case NOT_ENOUGH_GOLD -> {
                            updateCurrencyLabels();
                            selectedItemDesc.setText("Not enough gold.");
                        }

                        case SAVE_NOT_FOUND -> {
                            updateCurrencyLabels();
                            selectedItemDesc.setText("No player data found.");
                        }
                    }
                }))
                .exceptionally(exception -> {
                    exception.printStackTrace();

                    Platform.runLater(() -> {
                        hideLoadingOverlay();
                        setShopLoading(false);
                        updateCurrencyLabels();
                        selectedItemDesc.setText("You cannot buy a pet.");
                    });

                    return null;
                });
    }

    private void equipPet(PetType pet) {
        if (shopLoading || !ownedPets.contains(pet)) {
            return;
        }

        setShopLoading(true);
        showLoadingOverlay("Equipping pet...");

        CompletableFuture
                .supplyAsync(() ->
                        shopDAO.equipItem(UserSession.getCurrentUserId(),
                                ITEM_TYPE_PET, pet.name()), DatabaseExecutor.getExecutor()
                )
                .thenAccept(success -> Platform.runLater(() -> {
                    hideLoadingOverlay();
                    setShopLoading(false);
                    updateCurrencyLabels();

                    if (!success) {
                        selectedItemDesc.setText("You don't own this pet yet.");
                        return;
                    }

                    PetSelectionManager.getInstance().selectPet(pet);

                    if (gameWorld != null) {
                        gameWorld.equipPet(pet);
                    }

                    selectedItemDesc.setText("Skin equipped" + pet.getDisplayName());
                    loadPetShop();
                }))
                .exceptionally(exception -> {
                    exception.printStackTrace();

                    Platform.runLater(() -> {
                        hideLoadingOverlay();
                        setShopLoading(false);
                        updateCurrencyLabels();
                        selectedItemDesc.setText("Pets cannot be equipped.");
                    });
                    return null;
                });
    }

    private void playPetSound(PetType pet) {
        SoundManager.getInstance().stopAllSFX();

        if (pet.getSoundPath() != null
                && !pet.getSoundPath().isBlank()) {
            SoundManager.getInstance().playSFX(pet.getSoundPath());
        } else {
            SoundManager.getInstance().playSFX("button");
        }
    }

    private void loadWeaponShop() {
        activeHeroRenderers.clear();
        activeRenderers.clear();
        itemGrid.getChildren().clear();

        WeaponType equippedWeapon = WeaponSelectionManager.getInstance().getSelectedWeapon();

        for (WeaponType weapon : WeaponType.values()) {
            VBox card = createWeaponCard(weapon, equippedWeapon);
            itemGrid.getChildren().add(card);
        }
    }

    private VBox createWeaponCard(WeaponType weapon, WeaponType equippedWeapon) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("item-card");

        boolean isOwned = ownedWeapons.contains(weapon);
        boolean isEquipped = (weapon == equippedWeapon);

        if (isEquipped) {
            card.getStyleClass().add("item-card-equipped");
        }

        if (!isOwned) {
            card.getStyleClass().add("item-card-locked");
        }

        Canvas canvas = new Canvas(SHOP_CANVAS_SIZE, SHOP_CANVAS_SIZE);
        Image weaponImage = loadImage(weapon.getImagePath());
        drawWeaponIcon(canvas, weaponImage);

        Label nameLabel = new Label(weapon.getDisplayName());
        nameLabel.getStyleClass().add("item-card-title");

        card.getChildren().addAll(canvas, nameLabel);

        card.setOnMouseClicked(e -> {
            if (shopLoading) {
                return;
            }

            playWeaponSound(weapon);

            itemGrid.getChildren().forEach(
                    node -> node.getStyleClass().remove("item-card-selected")
            );
            card.getStyleClass().add("item-card-selected");

            this.selectedWeapon = weapon;
            this.selectedPet = null;

            selectedItemName.setText(weapon.getDisplayName());

            boolean isCurrentOwned = ownedWeapons.contains(weapon);
            boolean isCurrentEquipped =
                    WeaponSelectionManager.getInstance().isSelected(weapon);

            String description = buildWeaponDesc(weapon);

            if (!isCurrentOwned) {
                description +=
                        " | PRICE: " + weapon.getPrice() + " GOLD";
            }

            selectedItemDesc.setText(description);
            actionButton.setVisible(true);

            if (isCurrentEquipped) {
                actionButton.setText("EQUIPPED");
                actionButton.setDisable(true);
                actionButton.setOnAction(null);

            } else if (isCurrentOwned) {
                actionButton.setText("EQUIP WEAPON");
                actionButton.setDisable(false);
                actionButton.setOnAction(evt -> {
                    playWeaponSound(weapon);
                    equipWeapon(weapon);
                });

            } else {
                actionButton.setText(
                        "BUY - " + weapon.getPrice() + " GOLD"
                );
                actionButton.setDisable(false);
                actionButton.setOnAction(evt -> {
                    SoundManager.getInstance().playSFX("button");
                    purchaseWeapon(weapon);
                });
            }
        });

        return card;
    }

    private void purchaseWeapon(WeaponType weapon) {
        if (shopLoading || ownedWeapons.contains(weapon)) {
            return;
        }

        int price = weapon.getPrice();
        setShopLoading(true);
        showLoadingOverlay("Buying weapon...");

        CompletableFuture.supplyAsync(() -> shopDAO.purchaseItem(UserSession.getCurrentUserId(),
                        UserSession.getCurrentUsername(), ITEM_TYPE_WEAPON, weapon.name(),
                        price), DatabaseExecutor.getExecutor()
                )
                .thenAccept(result -> Platform.runLater(() -> {
                    hideLoadingOverlay();
                    setShopLoading(false);

                    switch (result) {
                        case SUCCESS -> {
                            ownedWeapons.add(weapon);
                            currentGold = Math.max(0, currentGold - price);
                            updateCurrencyLabels();

                            selectedItemName.setText(weapon.getDisplayName());
                            selectedItemDesc.setText("PURCHASE SUCCESS");

                            loadWeaponShop();
                        }

                        case ALREADY_OWNED -> {
                            ownedWeapons.add(weapon);
                            updateCurrencyLabels();
                            selectedItemDesc.setText("You already own this weapon.");
                            loadWeaponShop();
                        }

                        case NOT_ENOUGH_GOLD -> {
                            updateCurrencyLabels();
                            selectedItemDesc.setText("Not enough gold.");
                        }

                        case SAVE_NOT_FOUND -> {
                            updateCurrencyLabels();
                            selectedItemDesc.setText("Khong tim thay du lieu nguoi choi.");
                        }
                    }
                }))
                .exceptionally(exception -> {
                    exception.printStackTrace();

                    Platform.runLater(() -> {
                        hideLoadingOverlay();
                        setShopLoading(false);
                        updateCurrencyLabels();
                        selectedItemDesc.setText("Weapons cannot be purchased.");
                    });

                    return null;
                });
    }

    private void equipWeapon(WeaponType weapon) {
        if (shopLoading || !ownedWeapons.contains(weapon)) {
            return;
        }

        setShopLoading(true);
        showLoadingOverlay("Equipping weapon...");

        CompletableFuture
                .supplyAsync(() -> shopDAO.equipItem(UserSession.getCurrentUserId(),
                                ITEM_TYPE_WEAPON, weapon.name()
                        ), DatabaseExecutor.getExecutor()
                )
                .thenAccept(success -> Platform.runLater(() -> {
                    hideLoadingOverlay();
                    setShopLoading(false);
                    updateCurrencyLabels();

                    if (!success) {
                        selectedItemDesc.setText("You don't have any weapons yet.");
                        return;
                    }

                    WeaponSelectionManager.getInstance().selectWeapon(weapon);

                    if (gameWorld != null) {
                        gameWorld.equipWeapon(weapon);
                    }

                    selectedItemDesc.setText("Skin equipped " + weapon.getDisplayName());
                    loadWeaponShop();
                }))
                .exceptionally(exception -> {
                    exception.printStackTrace();

                    Platform.runLater(() -> {
                        hideLoadingOverlay();
                        setShopLoading(false);
                        updateCurrencyLabels();
                        selectedItemDesc.setText("Cannot be equipped with weapons.");
                    });

                    return null;
                });
    }

    private void playWeaponSound(WeaponType weapon) {
        SoundManager.getInstance().stopAllSFX();

        if (weapon.getSoundPath() != null
                && !weapon.getSoundPath().isBlank()) {
            SoundManager.getInstance().playSFX(
                    weapon.getSoundPath()
            );
        } else {
            SoundManager.getInstance().playSFX("button");
        }
    }

    private String buildWeaponDesc(WeaponType weapon) {
        String type = weapon.isRanged() ? "Gun" : "Melee";
        return "DMG: " + weapon.getDamage()
                + " | CD: " + weapon.getCooldownSeconds() + "s"
                + " | " + type;
    }

    // Ve anh vu khi (tinh) len canvas, giu ti le va khong lam mo pixel
    private void drawWeaponIcon(Canvas canvas, Image image) {
        if (image == null || image.getWidth() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setImageSmoothing(false);

        double scale = Math.min(
                SHOP_PET_SIZE / image.getWidth(),
                SHOP_PET_SIZE / image.getHeight()
        );
        double drawW = image.getWidth() * scale;
        double drawH = image.getHeight() * scale;
        double drawX = (canvas.getWidth() - drawW) / 2.0;
        double drawY = (canvas.getHeight() - drawH) / 2.0;

        gc.drawImage(image, drawX, drawY, drawW, drawH);
    }

    private void setupAnimationLoop() {
        shopAnimTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double deltaSeconds = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                elapsedTime += deltaSeconds;

                renderPetAnimations();
                renderHeroAnimations();
            }
        };
    }

    private void renderPetAnimations() {
        for (PetCanvasRenderer renderer : activeRenderers) {
            PetType pet = renderer.pet();
            Canvas canvas = renderer.canvas();
            Image sheet = renderer.idleSheet();

            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            int totalFrames = pet.getIdleFrameCount();
            if (totalFrames <= 0) totalFrames = 1;

            int currentFrame = (int) (elapsedTime / pet.getFrameDuration()) % totalFrames;
            double sx = currentFrame * pet.getFrameWidth();
            double sy = 0;
            double sw = pet.getFrameWidth();
            double sh = pet.getFrameHeight();
            double drawX = (canvas.getWidth() - SHOP_PET_SIZE) / 2.0;
            double drawY = (canvas.getHeight() - SHOP_PET_SIZE) / 2.0;

            gc.setImageSmoothing(false);
            gc.drawImage(sheet, sx, sy, sw, sh,
                    drawX, drawY, SHOP_PET_SIZE, SHOP_PET_SIZE
            );
        }
    }
    private void renderHeroAnimations() {
        for (HeroCanvasRenderer renderer : activeHeroRenderers) {
            HeroType hero = renderer.hero();
            Canvas canvas = renderer.canvas();
            Image sheet = renderer.idleSheet();

            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setImageSmoothing(false);

            int totalFrames = hero.getIdleFrameCount();
            if (totalFrames <= 0) totalFrames = 1;

            double frameDuration = hero.getFrameDuration();
            if (frameDuration <= 0) frameDuration = 0.15;

            int currentFrame =
                    (int) (elapsedTime / frameDuration) % totalFrames;

            double frameWidth = hero.getFrameWidth();
            double frameHeight = hero.getFrameHeight();
            double sourceX = currentFrame * frameWidth;

            double drawSize = 70.0;
            double drawX = (canvas.getWidth() - drawSize) / 2.0;
            double drawY = (canvas.getHeight() - drawSize) / 2.0;

            gc.drawImage(sheet, sourceX, 0, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
        }
    }

    private void startAnimation() {
        lastTime = 0;
        shopAnimTimer.start();
    }

    private void stopAnimation() {
        if (shopAnimTimer != null) {
            shopAnimTimer.stop();
        }
    }

    private void loadPlaceholderCategory(String title, String desc) {
        activeRenderers.clear();
        itemGrid.getChildren().clear();
        selectedItemName.setText(title);
        selectedItemDesc.setText(desc);
        actionButton.setVisible(false);
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;

        Image cachedImage = imageCache.get(path);
        if (cachedImage != null) return cachedImage;

        URL resource = getClass().getResource(path);
        if (resource == null) {
            System.err.println("Khong tim thay anh shop: " + path);
            return null;
        }

        Image image = new Image(resource.toExternalForm(), false);
        imageCache.put(path, image);
        return image;
    }

    private record ShopData(Set<String> petCodes, Set<String> weaponCodes, Set<String> heroCodes, int gold, int gems) {}
    private void addOwnedHeroes(Set<String> heroCodes) {
        ownedHeroes.clear();

        for (String code : heroCodes) {
            try {
                ownedHeroes.add(HeroType.valueOf(code));
            } catch (IllegalArgumentException ignored) {
                System.err.println("Hero trong DB khong ton tai: " + code);
            }
        }
    }
    private void loadHeroShop() {
        activeRenderers.clear();
        activeHeroRenderers.clear();
        itemGrid.getChildren().clear();

        HeroType equippedHero = HeroSelectionManager.getInstance().getSelectedHero();

        for (HeroType hero : HeroType.values()) {
            VBox card = createHeroCard(hero, equippedHero);
            itemGrid.getChildren().add(card);
        }
    }
    private VBox createHeroCard(HeroType hero, HeroType equippedHero) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("item-card");

        boolean isOwned = ownedHeroes.contains(hero);
        boolean isEquipped = hero == equippedHero;

        if (isEquipped) {
            card.getStyleClass().add("item-card-equipped");
        }

        if (!isOwned) {
            card.getStyleClass().add("item-card-locked");
        }

        Canvas canvas = new Canvas(SHOP_CANVAS_SIZE, SHOP_CANVAS_SIZE);
        Image idleSheet = loadImage(hero.getIdleSpritePath());

        if (idleSheet != null) {
            activeHeroRenderers.add(new HeroCanvasRenderer(hero, canvas, idleSheet));
        }

        Label nameLabel = new Label(hero.getDisplayName());

        nameLabel.getStyleClass().add("item-card-title");
        card.getChildren().addAll(canvas, nameLabel);

        card.setOnMouseClicked(event -> {
            if (shopLoading) return;

            SoundManager.getInstance().playSFX("button");

            itemGrid.getChildren().forEach(node -> node.getStyleClass().remove("item-card-selected"));

            card.getStyleClass().add("item-card-selected");

            selectedItemName.setText(hero.getDisplayName()
            );
            boolean currentlyOwned = ownedHeroes.contains(hero);

            boolean currentlyEquipped = HeroSelectionManager.getInstance().isSelected(hero);

            String description = "HP: " + hero.getMaxHealth() + " | ENERGY: " + hero.getMaxEnergy();

            if (!currentlyOwned) {
                description += " | PRICE: " + hero.getPrice() + " GOLD";
            }

            selectedItemDesc.setText(description);
            actionButton.setVisible(true);

            if (currentlyEquipped) {
                actionButton.setText("EQUIPPED");
                actionButton.setDisable(true);
                actionButton.setOnAction(null);

            } else if (currentlyOwned) {
                actionButton.setText("EQUIP HERO");
                actionButton.setDisable(false);
                actionButton.setOnAction(e -> equipHero(hero));

            } else {
                actionButton.setText("BUY - " + hero.getPrice() + " GOLD");

                actionButton.setDisable(false);
                actionButton.setOnAction(e -> purchaseHero(hero)
                );
            }
        });

        return card;
    }
    private void drawHeroIcon(Canvas canvas, Image image) {
        if (image == null || image.getWidth() <= 0) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setImageSmoothing(false);

        double frameWidth = 64.0;
        double frameHeight = 64.0;
        double drawSize = 70.0;

        double drawX = (canvas.getWidth() - drawSize) / 2.0;

        double drawY = (canvas.getHeight() - drawSize) / 2.0;

        gc.drawImage(image, 0, 0, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
    }
    private void purchaseHero(HeroType hero) {
        if (shopLoading || ownedHeroes.contains(hero)) {
            return;
        }

        int price = hero.getPrice();
        setShopLoading(true);
        showLoadingOverlay("Buying hero...");

        CompletableFuture.supplyAsync(() -> shopDAO.purchaseItem(UserSession.getCurrentUserId(),
                        UserSession.getCurrentUsername(), ITEM_TYPE_HERO,
                        hero.name(), price), DatabaseExecutor.getExecutor())
                .thenAccept(result ->
                        Platform.runLater(() -> {
                            hideLoadingOverlay();
                            setShopLoading(false);

                            switch (result) {
                                case SUCCESS -> {
                                    ownedHeroes.add(hero);
                                    currentGold = Math.max(0, currentGold - price);

                                    updateCurrencyLabels();
                                    selectedItemDesc.setText("PURCHASE SUCCESS");

                                    loadHeroShop();
                                }

                                case ALREADY_OWNED -> {
                                    ownedHeroes.add(hero);
                                    selectedItemDesc.setText("You already own this hero.");
                                    loadHeroShop();
                                }
                                case NOT_ENOUGH_GOLD -> selectedItemDesc.setText("Not enough gold.");
                                case SAVE_NOT_FOUND -> selectedItemDesc.setText("No save file found.");
                            }
                        })
                )
                .exceptionally(exception -> {exception.printStackTrace();

                    Platform.runLater(() -> {
                        hideLoadingOverlay();
                        setShopLoading(false);
                        selectedItemDesc.setText("Cannot buy heroes.");
                    });

                    return null;
                });
    }
    private void equipHero(HeroType hero) {
        if (shopLoading || !ownedHeroes.contains(hero)) {
            return;
        }

        setShopLoading(true);
        showLoadingOverlay("Equipping hero...");

        CompletableFuture
                .supplyAsync(() ->
                        shopDAO.equipItem(UserSession.getCurrentUserId(),
                                ITEM_TYPE_HERO, hero.name()), DatabaseExecutor.getExecutor())
                .thenAccept(success ->
                        Platform.runLater(() -> {
                            hideLoadingOverlay();
                            setShopLoading(false);

                            if (!success) {
                                selectedItemDesc.setText("You don't own this hero yet.");
                                return;
                            }

                            HeroSelectionManager.getInstance().selectHero(hero);
                            selectedItemDesc.setText("Skin equipped" + hero.getDisplayName());

                            loadHeroShop();
                        })
                )
                .exceptionally(exception -> {
                    exception.printStackTrace();

                    Platform.runLater(() -> {
                        hideLoadingOverlay();
                        setShopLoading(false);
                        selectedItemDesc.setText("Cannot equip heroes.");
                    });

                    return null;
                });
    }

}