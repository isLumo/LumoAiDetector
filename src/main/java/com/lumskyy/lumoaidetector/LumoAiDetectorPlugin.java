package com.lumskyy.lumoaidetector;

import com.lumskyy.lumoaidetector.command.LadCommand;
import com.lumskyy.lumoaidetector.config.PluginSettings;
import com.lumskyy.lumoaidetector.dataset.DatasetService;
import com.lumskyy.lumoaidetector.dataset.RecordingService;
import com.lumskyy.lumoaidetector.detector.DetectionListener;
import com.lumskyy.lumoaidetector.detector.DetectionService;
import com.lumskyy.lumoaidetector.message.MessageService;
import com.lumskyy.lumoaidetector.ml.ModelRepository;
import com.lumskyy.lumoaidetector.ml.ModelService;
import com.lumskyy.lumoaidetector.platform.Platform;
import com.lumskyy.lumoaidetector.storage.RuntimeStateService;
import com.lumskyy.lumoaidetector.storage.StatsService;
import com.lumskyy.lumoaidetector.util.NamedThreadFactory;
import com.lumskyy.lumoaidetector.util.ResourceService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LumoAiDetectorPlugin extends JavaPlugin {
    private volatile PluginSettings settings;
    private MessageService messages;
    private Platform platform;
    private ExecutorService ioExecutor;
    private ThreadPoolExecutor predictExecutor;
    private ScheduledExecutorService timerExecutor;
    private ExecutorService trainExecutor;
    private StatsService statsService;
    private DatasetService datasetService;
    private RecordingService recordingService;
    private ModelRepository modelRepository;
    private RuntimeStateService runtimeStateService;
    private ModelService modelService;
    private DetectionService detectionService;

    @Override
    public void onEnable() {
        ResourceService.saveIfMissing(this, "config.yml");
        ResourceService.saveIfMissing(this, "messages.yml");
        reloadConfig();
        this.settings = PluginSettings.from(getConfig());
        this.messages = new MessageService(this);
        this.messages.reload();
        this.platform = new Platform(this);
        this.ioExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("LumoAiDetector-IO"));
        int predictThreads = Math.max(1, settings.predictionThreads);
        this.predictExecutor = new ThreadPoolExecutor(predictThreads, predictThreads, 30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000), new NamedThreadFactory("LumoAiDetector-Predict"),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        this.predictExecutor.allowCoreThreadTimeOut(true);
        this.timerExecutor = Executors.newScheduledThreadPool(2, new NamedThreadFactory("LumoAiDetector-Timer"));
        this.trainExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("LumoAiDetector-Train"));
        this.statsService = new StatsService(this);
        this.statsService.load();
        this.statsService.startAutoSave(timerExecutor, settings);
        this.datasetService = new DatasetService(this, settings, ioExecutor, statsService);
        this.recordingService = new RecordingService(datasetService);
        this.modelRepository = new ModelRepository(this, settings);
        this.runtimeStateService = new RuntimeStateService(this);
        this.modelService = new ModelService(this, settings, messages, platform, modelRepository, runtimeStateService, timerExecutor, trainExecutor, statsService);
        this.detectionService = new DetectionService(settings, messages, platform, recordingService, modelService, statsService, predictExecutor);
        getServer().getPluginManager().registerEvents(new DetectionListener(detectionService, this), this);
        LadCommand ladCommand = new LadCommand(this);
        PluginCommand command = getCommand("lad");
        if (command != null) {
            command.setExecutor(ladCommand);
            command.setTabCompleter(ladCommand);
        }
        modelService.loadStartupModel();
        modelRepository.purgeExpiredBackups();
    }

    @Override
    public void onDisable() {
        // 1. Stop accepting new prediction work.
        if (predictExecutor != null) {
            predictExecutor.shutdownNow();
        }
        // 2. Stop accepting new dataset writes, then drain queued writes.
        if (datasetService != null) {
            datasetService.stopAcceptingWrites();
        }
        if (ioExecutor != null) {
            ioExecutor.shutdown();
            try {
                if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    getLogger().warning("IO executor did not finish in 10s, some writes may be lost.");
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        // 3. Close the writer only after the queue is drained, so no task re-opens it.
        if (datasetService != null) {
            datasetService.shutdown();
        }
        // 4. Persist stats.
        if (statsService != null) {
            statsService.save();
        }
        // 5. Stop the remaining executors.
        if (timerExecutor != null) {
            timerExecutor.shutdownNow();
        }
        if (trainExecutor != null) {
            trainExecutor.shutdownNow();
        }
    }

    public synchronized void reloadLumo() {
        reloadConfig();
        this.settings = PluginSettings.from(getConfig());
        this.messages.reload();
        this.statsService.startAutoSave(timerExecutor, settings);
        this.datasetService.updateSettings(settings);
        this.modelRepository.updateSettings(settings);
        this.modelService.updateSettings(settings);
        this.detectionService.updateSettings(settings);
        this.modelRepository.purgeExpiredBackups();
    }

    public PluginSettings settings() {
        return settings;
    }

    public MessageService messages() {
        return messages;
    }

    public Platform platform() {
        return platform;
    }

    public StatsService statsService() {
        return statsService;
    }

    public DatasetService datasetService() {
        return datasetService;
    }

    public RecordingService recordingService() {
        return recordingService;
    }

    public ModelRepository modelRepository() {
        return modelRepository;
    }

    public ModelService modelService() {
        return modelService;
    }

    public DetectionService detectionService() {
        return detectionService;
    }
}
