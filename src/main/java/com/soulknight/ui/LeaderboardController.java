package com.soulknight.ui;

import com.soulknight.database.LeaderboardEntry;
import com.soulknight.database.PlayerSaveDAO;
import com.soulknight.utils.DatabaseExecutor;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
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
                new ReadOnlyStringWrapper(data.getValue().playerName())
        );

        scoreColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(data.getValue().score())
        );

        goldColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(data.getValue().gold())
        );
    }

    public void setup(Runnable onClose, Consumer<String> onShowLoading, Runnable onHideLoading) {
        this.onClose = onClose;
        this.onShowLoading = onShowLoading;
        this.onHideLoading = onHideLoading;

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        if (onShowLoading != null) {
            onShowLoading.accept("Loading leaderboard...");
        }

        Task<List<LeaderboardEntry>> task = new Task<>() {
            @Override
            protected List<LeaderboardEntry> call() {
                return playerSaveDAO.getLeaderboard(20);
            }
        };

        task.setOnSucceeded(event -> {
            hideLoading();

            List<LeaderboardEntry> entries = task.getValue();

            if (entries == null) {
                leaderboardTable.getItems().clear();
                return;
            }

            leaderboardTable.getItems().setAll(entries);
        });

        task.setOnFailed(event -> {
            hideLoading();
            leaderboardTable.getItems().clear();

            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        DatabaseExecutor.getExecutor().execute(task);
    }

    private void hideLoading() {
        if (onHideLoading != null) {
            onHideLoading.run();
        }
    }

    @FXML
    private void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
    }
}