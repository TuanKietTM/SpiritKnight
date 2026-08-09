package com.soulknight.cache;

import com.soulknight.database.LeaderboardEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cache bang xep hang trong RAM.
 *
 * Leaderboard la du lieu chung cua server nen khong nen cache
 * ca session nhu inventory/shop.
 *
 * Cache chi duoc coi la moi trong mot khoang thoi gian ngan.
 */
public final class LeaderboardCache {

    private static final LeaderboardCache INSTANCE =
            new LeaderboardCache();

    /*
     * Cache ton tai 15 giay.
     *
     * Trong 15 giay neu mo Leaderboard lai
     * thi khong can query database.
     */
    private static final long CACHE_TTL_MS = 15_000L;
    private List<LeaderboardEntry> entries = new ArrayList<>();

    /*
     * Thoi diem lan cuoi load thanh cong tu database.
     */
    private long loadedAt;

    /*
     * Co du lieu cache hay chua.
     */
    private boolean loaded;
    /*
     * Dang co request refresh database hay khong.
     * Dung de tranh gui nhieu query trung nhau.
     */
    private boolean refreshInProgress;

    private LeaderboardCache() {
    }

    public static LeaderboardCache getInstance() {
        return INSTANCE;
    }

    /**
     * Kiem tra cache da co du lieu hay chua.
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * Kiem tra cache van con moi.
     */
    public synchronized boolean isFresh() {

        if (!loaded) {
            return false;
        }

        long age = System.currentTimeMillis() - loadedAt;
        return age < CACHE_TTL_MS;
    }

    /**
     * Cache co du lieu nhung co the da cu.
     *
     * Ham nay rat huu ich cho stale-while-revalidate:
     *
     * - hien du lieu cu ngay
     * - sau do refresh database ngam
     */
    public synchronized boolean hasData() {
        return loaded && entries != null && !entries.isEmpty();
    }

    /**
     * Lay snapshot cua leaderboard.
     *
     * Khong tra truc tiep List ben trong
     * de controller khong the sua cache ngoai y muon.
     */
    public synchronized List<LeaderboardEntry> get() {

        return new ArrayList<>(
                entries
        );
    }

    /**
     * Luu ket qua moi tu database vao cache.
     */
    public synchronized void set(List<LeaderboardEntry> newEntries) {
        entries.clear();

        if (newEntries != null) {
            entries.addAll(newEntries);
        }

        loadedAt =
                System.currentTimeMillis();

        loaded = true;
    }

    /**
     * Danh dau dang refresh.
     *
     * return true:
     * caller duoc phep bat dau query.
     *
     * return false:
     * da co mot query khac dang chay.
     */
    public synchronized boolean beginRefresh() {

        if (refreshInProgress) {
            return false;
        }

        refreshInProgress = true;

        return true;
    }

    /**
     * Goi khi query database ket thuc,
     * bat ke thanh cong hay that bai.
     */
    public synchronized void endRefresh() {
        refreshInProgress = false;
    }

    public synchronized boolean isRefreshInProgress() {
        return refreshInProgress;
    }

    /**
     * Tuoi cua cache tinh bang milliseconds.
     */
    public synchronized long getAgeMillis() {

        if (!loaded) {
            return Long.MAX_VALUE;
        }

        return Math.max(
                0L,
                System.currentTimeMillis()
                        - loadedAt
        );
    }

    /**
     * Xoa toan bo cache.
     */
    public synchronized void clear() {
        entries.clear();
        loadedAt = 0L;
        loaded = false;
        refreshInProgress = false;
    }
}