package com.lumskyy.lumoaidetector.ml;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import com.lumskyy.lumoaidetector.config.PluginSettings;
import com.lumskyy.lumoaidetector.util.Formats;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import smile.classification.RandomForest;

public final class ModelRepository {
    private static final int MAX_NAME_LENGTH = 128;
    private final LumoAiDetectorPlugin plugin;
    private volatile PluginSettings settings;

    public ModelRepository(LumoAiDetectorPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        modelsDir().mkdirs();
        backupsDir().mkdirs();
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public File modelsDir() {
        return new File(plugin.getDataFolder(), "models");
    }

    public File backupsDir() {
        return new File(plugin.getDataFolder(), "backups/models");
    }

    public List<ModelInfo> listModels() {
        File[] files = modelsDir().listFiles();
        List<ModelInfo> models = new ArrayList<ModelInfo>();
        if (files == null) {
            return models;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".bin")) {
                continue;
            }
            String name = stripBin(file.getName());
            File metadata = metadataFile(modelsDir(), name);
            models.add(new ModelInfo(name, file, metadata, ModelMetadata.load(metadata, file)));
        }
        Collections.sort(models, new Comparator<ModelInfo>() {
            @Override
            public int compare(ModelInfo first, ModelInfo second) {
                return Long.compare(second.metadata().createdAt(), first.metadata().createdAt());
            }
        });
        return models;
    }

    public List<BackupInfo> listBackups() {
        File[] files = backupsDir().listFiles();
        List<BackupInfo> backups = new ArrayList<BackupInfo>();
        if (files == null) {
            return backups;
        }
        long now = System.currentTimeMillis();
        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".bin")) {
                continue;
            }
            String name = stripBin(file.getName());
            File metadata = metadataFile(backupsDir(), name);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(metadata);
            long deletedAt = config.getLong("backup.deleted-at", file.lastModified());
            String originalName = config.getString("backup.original-name", originalFromBackupName(name));
            long expiresAt = deletedAt + settings.backupRetentionHours * 3600000L;
            if (expiresAt < now) {
                deleteQuietly(file);
                deleteQuietly(metadata);
                continue;
            }
            backups.add(new BackupInfo(name, originalName, file, metadata, deletedAt, expiresAt));
        }
        Collections.sort(backups, new Comparator<BackupInfo>() {
            @Override
            public int compare(BackupInfo first, BackupInfo second) {
                return Long.compare(second.deletedAt(), first.deletedAt());
            }
        });
        return backups;
    }

    public ModelInfo findModel(String rawName) {
        String name = cleanName(rawName);
        if (name == null) {
            return null;
        }
        File file = modelFile(modelsDir(), name);
        if (!file.exists()) {
            return null;
        }
        File metadata = metadataFile(modelsDir(), name);
        return new ModelInfo(name, file, metadata, ModelMetadata.load(metadata, file));
    }

    public BackupInfo findBackup(String rawName) {
        String name = cleanName(rawName);
        if (name == null) {
            return null;
        }
        File file = modelFile(backupsDir(), name);
        if (!file.exists()) {
            return null;
        }
        File metadata = metadataFile(backupsDir(), name);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(metadata);
        long deletedAt = config.getLong("backup.deleted-at", file.lastModified());
        return new BackupInfo(name, config.getString("backup.original-name", originalFromBackupName(name)), file, metadata, deletedAt, deletedAt + settings.backupRetentionHours * 3600000L);
    }

    public void saveModel(RandomForest model, ModelMetadata metadata) throws IOException {
        modelsDir().mkdirs();
        File modelFile = modelFile(modelsDir(), metadata.name());
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(modelFile))) {
            output.writeObject(model);
        }
        String sha256 = computeSha256(modelFile);
        metadata.saveWithSha256(metadataFile(modelsDir(), metadata.name()), sha256);
    }

    public ModelBundle loadModel(String rawName) throws IOException, ClassNotFoundException {
        ModelInfo info = findModel(rawName);
        if (info == null) {
            return null;
        }
        String currentSha = computeSha256(info.modelFile());
        File metadataFile = info.metadataFile();
        if (metadataFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(metadataFile);
            String storedSha = config.getString("sha256", "");
            // Integrity/corruption check only. The hash lives next to the file, so
            // anyone who can replace the .bin can also rewrite this hash; this is not
            // tamper protection, only detection of accidental corruption.
            if (!storedSha.isEmpty() && !storedSha.equals(currentSha)) {
                plugin.getLogger().warning("SHA-256 mismatch for model " + rawName + ". File may be corrupted.");
                throw new IOException("Model file SHA-256 mismatch: file may be corrupted");
            }
        }
        // Constrain deserialization to an allow-list of model/JDK classes so a
        // malicious .bin cannot run gadget chains during readObject().
        try (ObjectInputStream input = new SecureObjectInputStream(new FileInputStream(info.modelFile()))) {
            Object object = input.readObject();
            if (!(object instanceof RandomForest)) {
                throw new IOException("File is not RandomForest model");
            }
            return new ModelBundle((RandomForest) object, info);
        }
    }

    public DeleteResult deleteToBackup(String rawName) throws IOException {
        ModelInfo info = findModel(rawName);
        if (info == null) {
            return null;
        }
        backupsDir().mkdirs();
        String backupName = info.name() + "__deleted__" + Formats.fileDate(System.currentTimeMillis());
        File backupModel = modelFile(backupsDir(), backupName);
        File backupMetadata = metadataFile(backupsDir(), backupName);
        Files.move(info.modelFile().toPath(), backupModel.toPath(), StandardCopyOption.REPLACE_EXISTING);
        if (info.metadataFile().exists()) {
            Files.move(info.metadataFile().toPath(), backupMetadata.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            info.metadata().save(backupMetadata);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(backupMetadata);
        config.set("backup.original-name", info.name());
        config.set("backup.deleted-at", System.currentTimeMillis());
        config.set("backup.deleted-readable", Formats.date(System.currentTimeMillis()));
        config.save(backupMetadata);
        purgeExpiredBackups();
        return new DeleteResult(info.name(), backupName);
    }

    public RestoreResult restoreBackup(String rawName) throws IOException {
        BackupInfo backup = findBackup(rawName);
        if (backup == null) {
            return null;
        }
        modelsDir().mkdirs();
        String modelName = uniqueModelName(backup.originalName());
        Files.move(backup.modelFile().toPath(), modelFile(modelsDir(), modelName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        File targetMetadata = metadataFile(modelsDir(), modelName);
        if (backup.metadataFile().exists()) {
            Files.move(backup.metadataFile().toPath(), targetMetadata.toPath(), StandardCopyOption.REPLACE_EXISTING);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(targetMetadata);
            config.set("name", modelName);
            config.set("file", modelName + ".bin");
            config.set("backup", null);
            config.save(targetMetadata);
        } else {
            ModelMetadata.minimal(modelName, modelFile(modelsDir(), modelName)).save(targetMetadata);
        }
        return new RestoreResult(backup.name(), modelName);
    }

    public boolean purgeBackup(String rawName) {
        BackupInfo backup = findBackup(rawName);
        if (backup == null) {
            return false;
        }
        deleteQuietly(backup.modelFile());
        deleteQuietly(backup.metadataFile());
        return true;
    }

    public void purgeExpiredBackups() {
        List<BackupInfo> backups = listBackups();
        long now = System.currentTimeMillis();
        for (BackupInfo backup : backups) {
            if (backup.expiresAt() < now) {
                deleteQuietly(backup.modelFile());
                deleteQuietly(backup.metadataFile());
            }
        }
        backups = listBackups();
        while (backups.size() > settings.maxBackups) {
            BackupInfo oldest = backups.get(backups.size() - 1);
            deleteQuietly(oldest.modelFile());
            deleteQuietly(oldest.metadataFile());
            backups.remove(backups.size() - 1);
        }
    }

    public String computeSha256(File file) {
        if (!file.exists()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(new FileInputStream(file), digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | IOException exception) {
            return "";
        }
    }

    private String uniqueModelName(String base) {
        String clean = cleanName(base);
        if (clean == null || clean.isEmpty()) {
            clean = "model-restored";
        }
        if (!modelFile(modelsDir(), clean).exists()) {
            return clean;
        }
        return clean + "-restored-" + Formats.fileDate(System.currentTimeMillis());
    }

    private File modelFile(File dir, String name) {
        return new File(dir, cleanName(name) + ".bin");
    }

    private File metadataFile(File dir, String name) {
        return new File(dir, cleanName(name) + ".yml");
    }

    static String cleanName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.trim().replace('\\', '/');
        if (name.contains("/") || name.contains("..")) {
            return null;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        if (name.endsWith(".bin")) {
            name = stripBin(name);
        }
        if (name.endsWith(".yml")) {
            name = name.substring(0, name.length() - 4);
        }
        if (!name.matches("[A-Za-z0-9._\\-]+")) {
            return null;
        }
        return name;
    }

    private static String stripBin(String name) {
        return name.endsWith(".bin") ? name.substring(0, name.length() - 4) : name;
    }

    private static String originalFromBackupName(String name) {
        int index = name.indexOf("__deleted__");
        return index > 0 ? name.substring(0, index) : name;
    }

    private void deleteQuietly(File file) {
        try {
            if (file.exists()) {
                Files.delete(file.toPath());
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Cannot delete " + file.getName() + ": " + exception.getMessage());
        }
    }
}
