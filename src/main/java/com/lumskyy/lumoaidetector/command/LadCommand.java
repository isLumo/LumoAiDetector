package com.lumskyy.lumoaidetector.command;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import com.lumskyy.lumoaidetector.dataset.DatasetSnapshot;
import com.lumskyy.lumoaidetector.dataset.RecordLabel;
import com.lumskyy.lumoaidetector.dataset.RecordSession;
import com.lumskyy.lumoaidetector.dataset.RecordToggleResult;
import com.lumskyy.lumoaidetector.detector.CheckResult;
import com.lumskyy.lumoaidetector.message.MessageService;
import com.lumskyy.lumoaidetector.ml.ActivationResult;
import com.lumskyy.lumoaidetector.ml.BackupInfo;
import com.lumskyy.lumoaidetector.ml.DeleteResult;
import com.lumskyy.lumoaidetector.ml.ModelInfo;
import com.lumskyy.lumoaidetector.ml.RestoreResult;
import com.lumskyy.lumoaidetector.util.Formats;
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
            status(sender);
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
        if (sub.equals("deactive")) {
            if (!require(sender, "LumoAiDetector.deactive")) {
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
            models(sender, page(args, 1));
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
            addRoot(sender, roots, "deactive", "LumoAiDetector.deactive");
            addRoot(sender, roots, "check", "LumoAiDetector.check");
            addRoot(sender, roots, "models", "LumoAiDetector.models");
            addRoot(sender, roots, "deleted", "LumoAiDetector.delete");
            addRoot(sender, roots, "backup", "LumoAiDetector.backup");
            return filter(roots, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.US);
        if (sub.equals("record")) {
            if (!has(sender, "LumoAiDetector.record")) {
                return Collections.emptyList();
            }
            if (args.length == 2) {
                return filter(list("legit", "cheater", "info"), args[1]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("info")) {
                return filter(list("all", "legit", "cheater"), args[2]);
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
        if ((sub.equals("deleted") || sub.equals("delete") || sub.equals("del")) && args.length == 2) {
            if (!has(sender, "LumoAiDetector.delete")) {
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
        String[] keys = new String[]{"help.line-reload", "help.line-status", "help.line-record", "help.line-record-info", "help.line-train", "help.line-active", "help.line-deactive", "help.line-check", "help.line-models", "help.line-deleted", "help.line-backup"};
        for (String key : keys) {
            sender.sendMessage(messages.get(key));
        }
    }

    private void status(CommandSender sender) {
        DatasetSnapshot snapshot = null;
        try {
            snapshot = plugin.datasetService().snapshot();
        } catch (Exception ignored) {
        }
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
        if (snapshot == null) {
            sender.sendMessage(messages.get("status.dataset", messages.placeholders("rows", "?", "legit", "?", "cheater", "?")));
        } else {
            sender.sendMessage(messages.get("status.dataset", messages.placeholders("rows", snapshot.rows(), "legit", snapshot.legitRows(), "cheater", snapshot.cheaterRows())));
        }
        sender.sendMessage(messages.get("status.stats", messages.placeholders("analyzed", plugin.statsService().analyzedWindows(), "alerts", plugin.statsService().alerts(), "definite", plugin.statsService().definiteCheaters())));
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
            messages.send(sender, "record.started", messages.placeholders("player", player.getName(), "label", label.id()));
        } else {
            messages.send(sender, "record.stopped", messages.placeholders("player", player.getName(), "windows", result.session().windows()));
        }
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
            models(sender, 1);
            return;
        }
        ActivationResult result = plugin.modelService().activate(args[1], false);
        if (result.status() == ActivationResult.Status.ACTIVATED || result.status() == ActivationResult.Status.ALREADY_ACTIVE) {
            messages.send(sender, "model.activated", messages.placeholders("model", result.activeName()));
        } else if (result.status() == ActivationResult.Status.CONFLICT) {
            messages.send(sender, "model.already-active", messages.placeholders("model", result.activeName()));
        } else if (result.status() == ActivationResult.Status.NOT_FOUND) {
            messages.send(sender, "model.not-found", messages.placeholders("model", args[1]));
        } else {
            messages.send(sender, "model.error", messages.placeholders("model", args[1], "error", result.error()));
        }
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
        CheckResult result = plugin.detectionService().check(player);
        if (!result.available()) {
            messages.send(sender, "check.no-data", messages.placeholders("player", player.getName()));
            return;
        }
        messages.send(sender, "check.result", messages.placeholders("player", player.getName(), "percent", Formats.percent(result.percent()), "windows", result.windows(), "confidence", Formats.percent(result.confidence()), "model", result.model()));
    }

    private void models(CommandSender sender, int page) {
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
                    new Button(messages.get("model.button-delete"), "/lad deleted " + info.name(), messages.get("model.hover-delete", messages.placeholders("model", info.name()))));
        }
        pageButtons(sender, "/lad models ", current, pages);
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            models(sender, 1);
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
        return sender.hasPermission("LumoAiDetector.admin") || sender.hasPermission("LumoAiDetector.reload") || sender.hasPermission("LumoAiDetector.status") || sender.hasPermission("LumoAiDetector.record") || sender.hasPermission("LumoAiDetector.train") || sender.hasPermission("LumoAiDetector.active") || sender.hasPermission("LumoAiDetector.deactive") || sender.hasPermission("LumoAiDetector.check") || sender.hasPermission("LumoAiDetector.models") || sender.hasPermission("LumoAiDetector.delete") || sender.hasPermission("LumoAiDetector.backup");
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
