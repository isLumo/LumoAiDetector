package com.lumskyy.lumoaidetector.command;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import com.lumskyy.lumoaidetector.dataset.DatasetCounts;
import com.lumskyy.lumoaidetector.dataset.RecordLabel;
import com.lumskyy.lumoaidetector.dataset.RecordSession;
import com.lumskyy.lumoaidetector.dataset.RecordToggleResult;
import com.lumskyy.lumoaidetector.detector.CheckResult;
import com.lumskyy.lumoaidetector.message.MessageService;
import com.lumskyy.lumoaidetector.ml.ActivationResult;
import com.lumskyy.lumoaidetector.ml.BackupInfo;
import com.lumskyy.lumoaidetector.ml.DeleteResult;
import com.lumskyy.lumoaidetector.ml.ModelInfo;
import com.lumskyy.lumoaidetector.ml.ModelMetadata;
import com.lumskyy.lumoaidetector.ml.RestoreResult;
import com.lumskyy.lumoaidetector.util.Formats;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class LadCommand implements CommandExecutor, TabCompleter {
    private static final int PAGE_SIZE = 8;
    private final LumoAiDetectorPlugin plugin;
    private final MessageService messages;

    public LadCommand(LumoAiDetectorPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.US);
        if (sub.equals("reload")) {
            if (!require(sender, "LumoAiDetector.reload")) {
                return true;
            }
            plugin.reloadLumo();
            messages.send(sender, "reload.success");
            return true;
        }
        if (sub.equals("status")) {
            if (!require(sender, "LumoAiDetector.status")) {
                return true;
            }
            statusAsync(sender);
            return true;
        }
        if (sub.equals("record") || sub.equals("recod")) {
            if (!require(sender, "LumoAiDetector.record")) {
                return true;
            }
            record(sender, args);
            return true;
        }
        if (sub.equals("train")) {
            if (!require(sender, "LumoAiDetector.train")) {
                return true;
            }
            plugin.modelService().train(sender);
            return true;
        }
        if (sub.equals("active")) {
            if (!require(sender, "LumoAiDetector.active")) {
                return true;
            }
            active(sender, args);
            return true;
        }
        if (sub.equals("deactive") || sub.equals("deactivate")) {
            if (!require(sender, "LumoAiDetector.deactivate")) {
                return true;
            }
            plugin.modelService().deactivate();
            messages.send(sender, "model.deactivated");
            return true;
        }
        if (sub.equals("check")) {
            if (!require(sender, "LumoAiDetector.check")) {
                return true;
            }
            check(sender, args);
            return true;
        }
        if (sub.equals("models")) {
            if (!require(sender, "LumoAiDetector.models")) {
                return true;
            }
            models(sender, args);
            return true;
        }
        if (sub.equals("deleted") || sub.equals("delete") || sub.equals("del")) {
            if (!require(sender, "LumoAiDetector.delete")) {
                return true;
            }
            delete(sender, args);
            return true;
        }
        if (sub.equals("backup") || sub.equals("backups")) {
            if (!require(sender, "LumoAiDetector.backup")) {
                return true;
            }
            backup(sender, args);
            return true;
        }
        if (sub.equals("dataset") || sub.equals("ds")) {
            if (!require(sender, "LumoAiDetector.status")) {
                return true;
            }
            dataset(sender, args);
            return true;
        }
        help(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAny(sender)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> roots = new ArrayList<String>();
            addRoot(sender, roots, "reload", "LumoAiDetector.reload");
            addRoot(sender, roots, "status", "LumoAiDetector.status");
            addRoot(sender, roots, "record", "LumoAiDetector.record");
            addRoot(sender, roots, "train", "LumoAiDetector.train");
            addRoot(sender, roots, "active", "LumoAiDetector.active");
            addRoot(sender, roots, "deactivate", "LumoAiDetector.deactivate");
            addRoot(sender, roots, "check", "LumoAiDetector.check");
            addRoot(sender, roots, "models", "LumoAiDetector.models");
            addRoot(sender, roots, "delete", "LumoAiDetector.delete");
            addRoot(sender, roots, "backup", "LumoAiDetector.backup");
            addRoot(sender, roots, "dataset", "LumoAiDetector.status");
            return filter(roots, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.US);
        if (sub.equals("record")) {
            if (!has(sender, "LumoAiDetector.record")) {
                return Collections.emptyList();
            }
            if (args.length == 2) {
                return filter(list("legit", "cheater", "info", "stop"), args[1]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("info")) {
                return filter(list("all", "legit", "cheater"), args[2]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("stop")) {
                return filter(list("all"), args[2]);
            }
            if (args.length == 3) {
                return filter(players(), args[2]);
            }
        }
        if (sub.equals("active") && args.length == 2) {
            if (!has(sender, "LumoAiDetector.active")) {
                return Collections.emptyList();
            }
            return filter(modelNames(), args[1]);
        }
        if (sub.equals("check") && args.length == 2) {
            if (!has(sender, "LumoAiDetector.check")) {
                return Collections.emptyList();
            }
            return filter(players(), args[1]);
        }
        if (sub.equals("check") && args.length == 3) {
            if (!has(sender, "LumoAiDetector.check")) {
                return Collections.emptyList();
            }
            return filter(list("history"), args[2]);
        }
        if (sub.equals("models") && args.length == 2) {
            if (!has(sender, "LumoAiDetector.models")) {
                return Collections.emptyList();
            }
            return filter(list("info", "compare"), args[1]);
        }
        if (sub.equals("models") && args.length == 3 && args[1].equalsIgnoreCase("info")) {
            if (!has(sender, "LumoAiDetector.models")) {
                return Collections.emptyList();
            }
            return filter(modelNames(), args[2]);
        }
        if (sub.equals("models") && (args.length == 3 || args.length == 4) && args[1].equalsIgnoreCase("compare")) {
            if (!has(sender, "LumoAiDetector.models")) {
                return Collections.emptyList();
            }
            return filter(modelNames(), args[args.length - 1]);
        }
        if ((sub.equals("deleted") || sub.equals("delete") || sub.equals("del")) && args.length == 2) {
            if (!has(sender, "LumoAiDetector.delete")) {
                return Collections.emptyList();
            }
            return filter(modelNames(), args[1]);
        }
        if (sub.equals("dataset") && args.length == 2) {
            if (!has(sender, "LumoAiDetector.status")) {
                return Collections.emptyList();
            }
            return filter(list("info", "trim"), args[1]);
        }
        if (sub.equals("backup")) {
            if (!has(sender, "LumoAiDetector.backup")) {
                return Collections.emptyList();
            }
            if (args.length == 2) {
                return filter(list("list", "restore", "purge"), args[1]);
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("restore") || args[1].equalsIgnoreCase("purge"))) {
                return filter(backupNames(), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private void help(CommandSender sender) {
        messages.send(sender, "help.header");
        String[] keys = new String[]{"help.line-reload", "help.line-status", "help.line-record", "help.line-record-info", "help.line-record-stop", "help.line-train", "help.line-active", "help.line-deactive", "help.line-check", "help.line-models", "help.line-models-info", "help.line-models-compare", "help.line-deleted", "help.line-dataset", "help.line-backup"};
        for (String key : keys) {
            sender.sendMessage(messages.get(key));
        }
    }

    private void statusAsync(final CommandSender sender) {
        plugin.datasetService().ioExecutor().submit(new Runnable() {
            @Override
            public void run() {
                DatasetCounts counts = null;
                try {
                    counts = plugin.datasetService().counts();
                } catch (Exception ignored) {
                }
                final DatasetCounts finalCounts = counts;
                plugin.platform().runGlobal(new Runnable() {
                    @Override
                    public void run() {
                        String active = plugin.modelService().activeModelName();
                        if (active == null || active.isEmpty()) {
                            active = messages.get("model.none-active");
                        }
                        messages.send(sender, "status.header");
                        sender.sendMessage(messages.get("status.version", messages.placeholders("version", plugin.getDescription().getVersion())));
                        sender.sendMessage(messages.get("status.platform", messages.placeholders("server", plugin.platform().serverName(), "folia", plugin.platform().isFolia())));
                        sender.sendMessage(messages.get("status.detector", messages.placeholders("detector", plugin.settings().detectorEnabled ? "on" : "off")));
                        sender.sendMessage(messages.get("status.model", messages.placeholders("model", active)));
                        sender.sendMessage(messages.get("status.training", messages.placeholders("training", plugin.modelService().isTraining() ? "on" : "off", "phase", plugin.modelService().trainingPhase())));
                        sender.sendMessage(messages.get("status.recording", messages.placeholders("recording", plugin.recordingService().activeCount())));
                        if (finalCounts == null) {
                            sender.sendMessage(messages.get("status.dataset", messages.placeholders("rows", "?", "legit", "?", "cheater", "?")));
                        } else {
                            sender.sendMessage(messages.get("status.dataset", messages.placeholders("rows", finalCounts.rows(), "legit", finalCounts.legitRows(), "cheater", finalCounts.cheaterRows())));
                        }
                        sender.sendMessage(messages.get("status.stats", messages.placeholders("analyzed", plugin.statsService().analyzedWindows(), "alerts", plugin.statsService().alerts(), "definite", plugin.statsService().definiteCheaters())));
                    }
                });
            }
        });
    }

    private void record(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "record.usage");
            return;
        }
        if (args[1].equalsIgnoreCase("info")) {
            RecordLabel filter = args.length >= 3 && !args[2].equalsIgnoreCase("all") ? RecordLabel.parse(args[2]) : null;
            int page = page(args, 3);
            recordInfo(sender, filter, page);
            return;
        }
        if (args[1].equalsIgnoreCase("stop")) {
            recordStop(sender, args);
            return;
        }
        RecordLabel label = RecordLabel.parse(args[1]);
        if (label == null) {
            messages.send(sender, "record.usage");
            return;
        }
        Player player;
        if (args.length >= 3) {
            player = Bukkit.getPlayer(args[2]);
        } else if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            messages.send(sender, "record.usage");
            return;
        }
        if (player == null) {
            messages.send(sender, "common.player-not-found", messages.placeholders("player", args.length >= 3 ? args[2] : ""));
            return;
        }
        RecordToggleResult result = plugin.recordingService().toggle(player, label);
        if (result.started()) {
            if (result.previousLabel() != null) {
                messages.send(sender, "record.stopped", messages.placeholders("player", player.getName(), "windows", "0"));
            }
            messages.send(sender, "record.started", messages.placeholders("player", player.getName(), "label", label.id()));
        } else {
            messages.send(sender, "record.stopped", messages.placeholders("player", player.getName(), "windows", result.session().windows()));
        }
    }

    private void recordStop(CommandSender sender, String[] args) {
        if (args.length >= 3 && args[2].equalsIgnoreCase("all")) {
            int count = plugin.recordingService().stopAll();
            messages.send(sender, "record.stopped-all", messages.placeholders("count", count));
            return;
        }
        Player player;
        if (args.length >= 3) {
            player = Bukkit.getPlayer(args[2]);
        } else if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            messages.send(sender, "record.usage");
            return;
        }
        if (player == null) {
            messages.send(sender, "common.player-not-found", messages.placeholders("player", args.length >= 3 ? args[2] : ""));
            return;
        }
        plugin.recordingService().stop(player.getUniqueId());
        messages.send(sender, "record.stopped", messages.placeholders("player", player.getName(), "windows", "0"));
    }

    private void recordInfo(CommandSender sender, RecordLabel label, int page) {
        List<RecordSession> sessions = plugin.recordingService().sessions(label);
        if (sessions.isEmpty()) {
            messages.send(sender, "record.info-empty");
            return;
        }
        int pages = pages(sessions.size());
        int current = clampPage(page, pages);
        messages.send(sender, "record.info-header", messages.placeholders("filter", label == null ? "all" : label.id(), "page", current, "pages", pages));
        int start = (current - 1) * PAGE_SIZE;
        int end = Math.min(sessions.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            RecordSession session = sessions.get(i);
            String line = messages.get("record.info-entry", messages.placeholders("player", session.playerName(), "label", session.label().id(), "windows", session.windows(), "time", Formats.duration(System.currentTimeMillis() - session.startedAt())));
            sendButtons(sender, line, new Button(messages.get("record.button-stop"), "/lad record " + session.label().id() + " " + session.playerName(), messages.get("record.hover-stop", messages.placeholders("player", session.playerName()))));
        }
        pageButtons(sender, "/lad record info " + (label == null ? "all" : label.id()) + " ", current, pages);
    }

    private void active(CommandSender sender, String[] args) {
        if (args.length < 2) {
            models(sender, args);
            return;
        }
        plugin.modelService().activateAsync(sender, args[1]);
    }

    private void check(CommandSender sender, String[] args) {
        if (args.length < 2) {
            help(sender);
            return;
        }
        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            messages.send(sender, "common.player-not-found", messages.placeholders("player", args[1]));
            return;
        }
        if (args.length >= 3 && args[2].equalsIgnoreCase("history")) {
            ArrayDeque<String> history = plugin.detectionService().alertHistory(player);
            if (history.isEmpty()) {
                messages.send(sender, "check.history-empty", messages.placeholders("player", player.getName()));
                return;
            }
            messages.send(sender, "check.history-header", messages.placeholders("player", player.getName()));
            for (String entry : history) {
                sender.sendMessage(entry);
            }
            return;
        }
        CheckResult result = plugin.detectionService().check(player);
        if (!result.available()) {
            messages.send(sender, "check.no-data", messages.placeholders("player", player.getName()));
            return;
        }
        messages.send(sender, "check.result", messages.placeholders("player", player.getName(), "percent", Formats.percent(result.percent()), "windows", result.windows(), "confidence", Formats.percent(result.confidence()), "model", result.model()));
    }

    private void models(CommandSender sender, String[] args) {
        if (args.length >= 3 && args[1].equalsIgnoreCase("info")) {
            modelsInfo(sender, args);
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("compare")) {
            modelsCompare(sender, args);
            return;
        }
        int page = page(args, 1);
        List<ModelInfo> models = plugin.modelRepository().listModels();
        if (models.isEmpty()) {
            messages.send(sender, "model.list-empty");
            return;
        }
        int pages = pages(models.size());
        int current = clampPage(page, pages);
        messages.send(sender, "model.list-header", messages.placeholders("page", current, "pages", pages));
        String active = plugin.modelService().activeModelName();
        int start = (current - 1) * PAGE_SIZE;
        int end = Math.min(models.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            ModelInfo info = models.get(i);
            boolean isActive = info.name().equals(active);
            String key = isActive ? "model.list-entry-active" : "model.list-entry";
            String line = messages.get(key, messages.placeholders("model", info.name(), "size", Formats.size(info.size()), "rows", info.metadata().rows(), "accuracy", Formats.percent(info.metadata().accuracy()), "created", Formats.date(info.metadata().createdAt())));
            sendButtons(sender, line,
                    new Button(messages.get("model.button-active"), "/lad active " + info.name(), messages.get("model.hover-active", messages.placeholders("model", info.name()))),
                    new Button(messages.get("model.button-delete"), "/lad delete " + info.name(), messages.get("model.hover-delete", messages.placeholders("model", info.name()))));
        }
        pageButtons(sender, "/lad models ", current, pages);
    }

    private void modelsInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            models(sender, args);
            return;
        }
        ModelInfo info = plugin.modelRepository().findModel(args[2]);
        if (info == null) {
            messages.send(sender, "model.info-not-found", messages.placeholders("model", args[2]));
            return;
        }
        ModelMetadata meta = info.metadata();
        messages.send(sender, "model.info-header", messages.placeholders("model", info.name()));
        sender.sendMessage(messages.get("model.info-entry", messages.placeholders("accuracy", Formats.percent(meta.accuracy()), "precision", Formats.percent(meta.precision()), "recall", Formats.percent(meta.recall()), "f1", Formats.percent(meta.f1()))));
        sender.sendMessage(messages.get("model.info-data", messages.placeholders("rows", meta.rows(), "trees", meta.treeCount(), "features", meta.featureCount(), "version", meta.pluginVersion())));
        sender.sendMessage(messages.get("model.info-metrics-source", messages.placeholders("source", meta.metricsHoldout() ? "holdout" : "train-only", "balanced", meta.balancedClasses() ? "yes" : "no", "seed", String.valueOf(meta.seed()))));
    }

    private void modelsCompare(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "model.compare-usage");
            return;
        }
        ModelInfo first = plugin.modelRepository().findModel(args[2]);
        if (first == null) {
            messages.send(sender, "model.info-not-found", messages.placeholders("model", args[2]));
            return;
        }
        ModelInfo second = plugin.modelRepository().findModel(args[3]);
        if (second == null) {
            messages.send(sender, "model.info-not-found", messages.placeholders("model", args[3]));
            return;
        }
        ModelMetadata a = first.metadata();
        ModelMetadata b = second.metadata();
        messages.send(sender, "model.compare-header", messages.placeholders("a", first.name(), "b", second.name()));
        sender.sendMessage(messages.get("model.compare-row", messages.placeholders("metric", "Accuracy", "a", Formats.percent(a.accuracy()), "b", Formats.percent(b.accuracy()))));
        sender.sendMessage(messages.get("model.compare-row", messages.placeholders("metric", "Precision", "a", Formats.percent(a.precision()), "b", Formats.percent(b.precision()))));
        sender.sendMessage(messages.get("model.compare-row", messages.placeholders("metric", "Recall", "a", Formats.percent(a.recall()), "b", Formats.percent(b.recall()))));
        sender.sendMessage(messages.get("model.compare-row", messages.placeholders("metric", "F1", "a", Formats.percent(a.f1()), "b", Formats.percent(b.f1()))));
        sender.sendMessage(messages.get("model.compare-row", messages.placeholders("metric", "Rows", "a", String.valueOf(a.rows()), "b", String.valueOf(b.rows()))));
    }

    private void dataset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "record.usage");
            return;
        }
        if (args[1].equalsIgnoreCase("trim")) {
            datasetTrim(sender, args);
            return;
        }
        if (!args[1].equalsIgnoreCase("info")) {
            messages.send(sender, "record.usage");
            return;
        }
        plugin.datasetService().ioExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    DatasetCounts counts = plugin.datasetService().counts();
                    long fileSize = plugin.datasetService().file().length();
                    int maxRows = plugin.settings().maxDatasetRows;
                    boolean limitHit = maxRows > 0 && counts.rows() >= maxRows;
                    plugin.platform().send(sender, messages.get("dataset.info", messages.placeholders(
                            "rows", counts.rows(),
                            "legit", counts.legitRows(),
                            "cheater", counts.cheaterRows(),
                            "skipped", counts.skippedRows(),
                            "size", Formats.size(fileSize)
                    )));
                    plugin.platform().send(sender, messages.get("dataset.info-limit", messages.placeholders(
                            "max", maxRows == 0 ? "unlimited" : String.valueOf(maxRows),
                            "limit", limitHit ? "yes" : "no"
                    )));
                } catch (Exception exception) {
                    plugin.platform().send(sender, messages.get("model.error", messages.placeholders("model", "dataset", "error", exception.getMessage())));
                }
            }
        });
    }

    private void datasetTrim(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "dataset.trim-usage");
            return;
        }
        int keepRows;
        try {
            keepRows = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            messages.send(sender, "dataset.trim-usage");
            return;
        }
        plugin.datasetService().trim(keepRows, sender);
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            models(sender, args);
            return;
        }
        String active = plugin.modelService().activeModelName();
        if (active != null && active.equals(args[1])) {
            messages.send(sender, "model.already-active", messages.placeholders("model", active));
            return;
        }
        try {
            DeleteResult result = plugin.modelRepository().deleteToBackup(args[1]);
            if (result == null) {
                messages.send(sender, "model.not-found", messages.placeholders("model", args[1]));
                return;
            }
            messages.send(sender, "model.deleted", messages.placeholders("model", result.modelName(), "backup", result.backupName()));
        } catch (Exception exception) {
            messages.send(sender, "model.error", messages.placeholders("model", args[1], "error", exception.getMessage()));
        }
    }

    private void backup(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            backupList(sender, page(args, 2));
            return;
        }
        if (args[1].equalsIgnoreCase("restore")) {
            if (args.length < 3) {
                messages.send(sender, "backup.usage");
                return;
            }
            try {
                RestoreResult result = plugin.modelRepository().restoreBackup(args[2]);
                if (result == null) {
                    messages.send(sender, "backup.not-found", messages.placeholders("backup", args[2]));
                    return;
                }
                messages.send(sender, "backup.restored", messages.placeholders("backup", result.backupName(), "model", result.modelName()));
            } catch (Exception exception) {
                messages.send(sender, "model.error", messages.placeholders("model", args[2], "error", exception.getMessage()));
            }
            return;
        }
        if (args[1].equalsIgnoreCase("purge")) {
            if (args.length < 3) {
                messages.send(sender, "backup.usage");
                return;
            }
            if (plugin.modelRepository().purgeBackup(args[2])) {
                messages.send(sender, "backup.purged", messages.placeholders("backup", args[2]));
            } else {
                messages.send(sender, "backup.not-found", messages.placeholders("backup", args[2]));
            }
            return;
        }
        messages.send(sender, "backup.usage");
    }

    private void backupList(CommandSender sender, int page) {
        List<BackupInfo> backups = plugin.modelRepository().listBackups();
        if (backups.isEmpty()) {
            messages.send(sender, "backup.list-empty");
            return;
        }
        int pages = pages(backups.size());
        int current = clampPage(page, pages);
        messages.send(sender, "backup.list-header", messages.placeholders("page", current, "pages", pages));
        int start = (current - 1) * PAGE_SIZE;
        int end = Math.min(backups.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            BackupInfo info = backups.get(i);
            long ttl = Math.max(0L, info.expiresAt() - System.currentTimeMillis());
            String line = messages.get("backup.list-entry", messages.placeholders("backup", info.name(), "size", Formats.size(info.size()), "ttl", Formats.duration(ttl)));
            sendButtons(sender, line,
                    new Button(messages.get("backup.button-restore"), "/lad backup restore " + info.name(), messages.get("backup.hover-restore", messages.placeholders("backup", info.name()))),
                    new Button(messages.get("backup.button-purge"), "/lad backup purge " + info.name(), messages.get("backup.hover-purge", messages.placeholders("backup", info.name()))));
        }
        pageButtons(sender, "/lad backup list ", current, pages);
    }

    private boolean require(CommandSender sender, String permission) {
        if (has(sender, permission)) {
            return true;
        }
        messages.send(sender, "common.no-permission", messages.placeholders("permission", permission));
        return false;
    }

    private boolean has(CommandSender sender, String permission) {
        return !(sender instanceof Player) || sender.hasPermission("LumoAiDetector.admin") || sender.hasPermission(permission);
    }

    private boolean hasAny(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.hasPermission("LumoAiDetector.admin") || sender.hasPermission("LumoAiDetector.reload") || sender.hasPermission("LumoAiDetector.status") || sender.hasPermission("LumoAiDetector.record") || sender.hasPermission("LumoAiDetector.train") || sender.hasPermission("LumoAiDetector.active") || sender.hasPermission("LumoAiDetector.deactivate") || sender.hasPermission("LumoAiDetector.check") || sender.hasPermission("LumoAiDetector.models") || sender.hasPermission("LumoAiDetector.delete") || sender.hasPermission("LumoAiDetector.backup");
    }

    private void addRoot(CommandSender sender, List<String> roots, String value, String permission) {
        if (has(sender, permission)) {
            roots.add(value);
        }
    }

    private int page(String[] args, int index) {
        if (args.length <= index) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private int pages(int total) {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int clampPage(int page, int pages) {
        return Math.max(1, Math.min(page, pages));
    }

    private void pageButtons(CommandSender sender, String commandPrefix, int current, int pages) {
        if (pages <= 1) {
            return;
        }
        List<Button> buttons = new ArrayList<Button>();
        if (current > 1) {
            buttons.add(new Button(messages.get("buttons.prev"), commandPrefix + (current - 1), messages.get("buttons.hover-prev", messages.placeholders("page", current - 1))));
        }
        if (current < pages) {
            buttons.add(new Button(messages.get("buttons.next"), commandPrefix + (current + 1), messages.get("buttons.hover-next", messages.placeholders("page", current + 1))));
        }
        sendButtons(sender, "", buttons.toArray(new Button[buttons.size()]));
    }

    private void sendButtons(CommandSender sender, String line, Button... buttons) {
        if (!(sender instanceof Player)) {
            StringBuilder builder = new StringBuilder(line);
            for (Button button : buttons) {
                builder.append(button.text).append(" ").append(button.command);
            }
            sender.sendMessage(builder.toString());
            return;
        }
        TextComponent root = new TextComponent("");
        addLegacy(root, line);
        for (Button button : buttons) {
            TextComponent component = new TextComponent("");
            addLegacy(component, button.text);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, button.command));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(button.hover).create()));
            root.addExtra(component);
        }
        ((Player) sender).spigot().sendMessage(root);
    }

    private void addLegacy(TextComponent root, String text) {
        BaseComponent[] components = TextComponent.fromLegacyText(text == null ? "" : text);
        for (BaseComponent component : components) {
            root.addExtra(component);
        }
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.US);
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase(Locale.US).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<String> list(String... values) {
        List<String> list = new ArrayList<String>();
        Collections.addAll(list, values);
        return list;
    }

    private List<String> players() {
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> modelNames() {
        List<String> names = new ArrayList<String>();
        for (ModelInfo info : plugin.modelRepository().listModels()) {
            names.add(info.name());
        }
        return names;
    }

    private List<String> backupNames() {
        List<String> names = new ArrayList<String>();
        for (BackupInfo info : plugin.modelRepository().listBackups()) {
            names.add(info.name());
        }
        return names;
    }

    private static final class Button {
        private final String text;
        private final String command;
        private final String hover;

        private Button(String text, String command, String hover) {
            this.text = text;
            this.command = command;
            this.hover = hover;
        }
    }
}
