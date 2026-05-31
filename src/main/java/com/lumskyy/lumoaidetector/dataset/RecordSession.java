package com.lumskyy.lumoaidetector.dataset;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class RecordSession {
    private final UUID uuid;
    private final String playerName;
    private final RecordLabel label;
    private final long startedAt;
    private final AtomicLong windows = new AtomicLong();

    public RecordSession(UUID uuid, String playerName, RecordLabel label) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.label = label;
        this.startedAt = System.currentTimeMillis();
    }

    public UUID uuid() {
        return uuid;
    }

    public String playerName() {
        return playerName;
    }

    public RecordLabel label() {
        return label;
    }

    public long startedAt() {
        return startedAt;
    }

    public long windows() {
        return windows.get();
    }

    public void incrementWindows() {
        windows.incrementAndGet();
    }
}
