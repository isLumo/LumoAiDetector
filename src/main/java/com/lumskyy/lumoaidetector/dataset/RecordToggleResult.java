package com.lumskyy.lumoaidetector.dataset;

public final class RecordToggleResult {
    private final boolean started;
    private final RecordSession session;

    public RecordToggleResult(boolean started, RecordSession session) {
        this.started = started;
        this.session = session;
    }

    public boolean started() {
        return started;
    }

    public RecordSession session() {
        return session;
    }
}
