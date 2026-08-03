package com.soulknight.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// tao thread dung rieng voi cac tac vu database de tranh treo giao dien
public final class DatabaseExecutor {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "database-worker");
        thread.setDaemon(true);
        return thread;
    });

    private DatabaseExecutor() {
    }

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}