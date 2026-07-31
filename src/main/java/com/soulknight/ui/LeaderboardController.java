package com.soulknight.ui;

import com.soulknight.database.LeaderboardEntry;
import com.soulknight.database.PlayerSaveDAO;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Consumer;

public final class LeaderboardController {

    @FXML private TableView<LeaderboardEntry> leaderboardTable;
    @FXML private TableColumn<LeaderboardEntry, Number> rankColumn;
    @FXML private TableColumn<LeaderboardEntry, String> nameColumn;
    @FXML private TableColumn<LeaderboardEntry, Number> scoreColumn;
    @FXML private TableColumn<LeaderboardEntry, Number> goldColumn;

    private final PlayerSaveDAO playerSaveDAO = new PlayerSaveDAO();

    private Runnable onClose;
    private Consumer<String> onShowLoading;
    private Runnable onHideLoading;

    @FXML
    public void initialize() {
        rankColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(data.getValue().rank())
        );

        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().playerName()
                )
        );

        scoreColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().score()
                )
        );

        goldColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(
                        data.getValue().gold()
                )
        );
    }

    public void setup(Runnable onClose,
                      Consumer<String> onShowLoading,
                      Runnable onHideLoading) {

        this.onClose = onClose;
        this.onShowLoading = onShowLoading;
        this.onHideLoading = onHideLoading;

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        if (onShowLoading != null) {
            onShowLoading.accept("Loading leaderboard...");
        }

        Thread thread = new Thread(() -> {
            try {
                List<LeaderboardEntry> entries =
                        playerSaveDAO.getLeaderboard(20);

                Platform.runLater(() -> {
                    if (onHideLoading != null) {
                        onHideLoading.run();
                    }

                    leaderboardTable.getItems().setAll(entries);
                });

            } catch (RuntimeException exception) {
                exception.printStackTrace();

                Platform.runLater(() -> {
                    if (onHideLoading != null) {
                        onHideLoading.run();
                    }
                });
            }
        }, "leaderboard-load-thread");

        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
    }
}