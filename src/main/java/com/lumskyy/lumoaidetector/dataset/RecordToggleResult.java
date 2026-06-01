package com.lumskyy.lumoaidetector.dataset;

public final class RecordToggleResult {
    private final boolean started;
    private final RecordSession session;
    private final RecordLabel previousLabel;

    public RecordToggleResult(boolean started, RecordSession session) {
        this(started, session, null);
    }

    public RecordToggleResult(boolean started, RecordSession session, RecordLabel previousLabel) {
        this.started = started;
        this.session = session;
        this.previousLabel = previousLabel;
    }

    public boolean started() {
        return started;
    }

    public RecordSession session() {
        return session;
    }

    public RecordLabel previousLabel() {
        return previousLabel;
    }
}
