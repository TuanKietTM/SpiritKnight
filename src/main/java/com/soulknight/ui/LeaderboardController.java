package com.soulknight.ui;

import com.soulknight.cache.LeaderboardCache;
import com.soulknight.database.LeaderboardEntry;
import com.soulknight.database.PlayerSaveDAO;
import com.soulknight.utils.DatabaseExecutor;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class LeaderboardController {

    @FXML private TableView<LeaderboardEntry> leaderboardTable;
    @FXML private TableColumn<LeaderboardEntry, Number> rankColumn;
    @FXML private TableColumn<LeaderboardEntry, String> nameColumn;
    @FXML private TableColumn<LeaderboardEntry, Number> scoreColumn;
    @FXML private TableColumn<LeaderboardEntry, Number> goldColumn;

    private final PlayerSaveDAO playerSaveDAO = new PlayerSaveDAO();
    private final LeaderboardCache leaderboardCache = LeaderboardCache.getInstance();

    private Runnable onClose;
    private Consumer<String> onShowLoading;
    private Runnable onHideLoading;
    private int waitCacheAttempts;

    @FXML
    public void initialize() {
        rankColumn.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().rank()));
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().playerName()));
        scoreColumn.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().score()));
        goldColumn.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().gold()));
    }

    public void setup(Runnable onClose, Consumer<String> onShowLoading, Runnable onHideLoading) {
        this.onClose = onClose;
        this.onShowLoading = onShowLoading;
        this.onHideLoading = onHideLoading;
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        // Cache con moi thi hien ngay, khong cham vao database.
        if (leaderboardCache.isFresh()) {
            renderEntries(leaderboardCache.get());
            hideLoading();
            return;
        }

        // Cache cu van duoc hien ngay, sau do refresh ngam de UI khong bi dung.
        if (leaderboardCache.hasData()) {
            renderEntries(leaderboardCache.get());
            hideLoading();
            refreshLeaderboard(false);
            return;
        }

        // Neu UIManager dang preload thi doi cache mot khoang ngan, tranh query DB trung lap.
        if (leaderboardCache.isRefreshInProgress()) {
            showLoading();
            waitCacheAttempts = 0;
            waitForPreloadCache();
            return;
        }

        showLoading();
        refreshLeaderboard(true);
    }

    private void refreshLeaderboard(boolean clearOnFail) {
        if (!leaderboardCache.beginRefresh()) {
            if (!leaderboardCache.hasData()) {
                waitCacheAttempts = 0;
                waitForPreloadCache();
            }
            return;
        }

        CompletableFuture.supplyAsync(() -> playerSaveDAO.getLeaderboard(20), DatabaseExecutor.getExecutor())
                .thenAccept(entries -> javafx.application.Platform.runLater(() -> {
                    leaderboardCache.set(entries != null ? entries : List.of());
                    leaderboardCache.endRefresh();
                    renderEntries(leaderboardCache.get());
                    hideLoading();
                }))
                .exceptionally(exception -> {
                    leaderboardCache.endRefresh();
                    exception.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        hideLoading();
                        if (clearOnFail && !leaderboardCache.hasData()) leaderboardTable.getItems().clear();
                    });
                    return null;
                });
    }

    private void waitForPreloadCache() {
        if (leaderboardCache.hasData() || leaderboardCache.isFresh()) {
            renderEntries(leaderboardCache.get());
            hideLoading();
            return;
        }

        // Preload da ket thuc nhung khong co du lieu thi controller tu tai lai.
        if (!leaderboardCache.isRefreshInProgress()) {
            refreshLeaderboard(true);
            return;
        }

        // Khong cho loading cho vo han neu ket noi database bi treo.
        if (++waitCacheAttempts > 50) {
            hideLoading();
            return;
        }

        PauseTransition wait = new PauseTransition(Duration.millis(100));
        wait.setOnFinished(event -> waitForPreloadCache());
        wait.play();
    }

    private void renderEntries(List<LeaderboardEntry> entries) {
        if (entries == null) {
            leaderboardTable.getItems().clear();
            return;
        }
        leaderboardTable.getItems().setAll(entries);
    }

    private void showLoading() {
        if (onShowLoading != null) onShowLoading.accept("Loading leaderboard...");
    }

    private void hideLoading() {
        if (onHideLoading != null) onHideLoading.run();
    }

    @FXML
    private void handleClose() {
        if (onClose != null) onClose.run();
    }
}