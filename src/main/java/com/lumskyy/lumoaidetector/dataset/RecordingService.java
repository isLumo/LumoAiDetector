package com.lumskyy.lumoaidetector.dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class RecordingService {
    private final DatasetService datasetService;
    private final Map<UUID, RecordSession> sessions = new ConcurrentHashMap<UUID, RecordSession>();

    public RecordingService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    public RecordToggleResult toggle(Player player, RecordLabel label) {
        RecordSession existing = sessions.get(player.getUniqueId());
        if (existing != null && existing.label() == label) {
            sessions.remove(player.getUniqueId());
            return new RecordToggleResult(false, existing);
        }
        RecordSession session = new RecordSession(player.getUniqueId(), player.getName(), label);
        sessions.put(player.getUniqueId(), session);
        return new RecordToggleResult(true, session);
    }

    public void stop(UUID uuid) {
        sessions.remove(uuid);
    }

    public void record(Player player, double[] features) {
        RecordSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        datasetService.appendWindow(session.label(), features);
        session.incrementWindows();
    }

    public int activeCount() {
        return sessions.size();
    }

    public List<RecordSession> sessions(RecordLabel label) {
        List<RecordSession> list = new ArrayList<RecordSession>();
        for (RecordSession session : sessions.values()) {
            if (label == null || session.label() == label) {
                list.add(session);
            }
        }
        Collections.sort(list, new Comparator<RecordSession>() {
            @Override
            public int compare(RecordSession first, RecordSession second) {
                return first.playerName().compareToIgnoreCase(second.playerName());
            }
        });
        return list;
    }
}
