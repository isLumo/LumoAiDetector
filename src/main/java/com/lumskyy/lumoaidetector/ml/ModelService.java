package com.lumskyy.lumoaidetector.ml;

import com.lumskyy.lumoaidetector.LumoAiDetectorPlugin;
import com.lumskyy.lumoaidetector.config.PluginSettings;
import com.lumskyy.lumoaidetector.dataset.DatasetCsv;
import com.lumskyy.lumoaidetector.dataset.DatasetSnapshot;
import com.lumskyy.lumoaidetector.message.MessageService;
import com.lumskyy.lumoaidetector.platform.Platform;
import com.lumskyy.lumoaidetector.storage.RuntimeStateService;
import com.lumskyy.lumoaidetector.storage.StatsService;
import com.lumskyy.lumoaidetector.util.Formats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import smile.classification.RandomForest;

public final class ModelService {
    private final LumoAiDetectorPlugin plugin;
    private final MessageService messages;
    private final Platform platform;
    private final ModelRepository repository;
    private final RuntimeStateService runtimeStateService;
    private final ScheduledExecutorService timerExecutor;
    private final ExecutorService trainExecutor;
    private final StatsService statsService;
    private volatile PluginSettings settings;
    private volatile RandomForest activeModel;
    private volatile ModelInfo activeInfo;
    private final AtomicBoolean training = new AtomicBoolean(false);
    private volatile String trainingPhase = "idle";
    private volatile long trainingStartedAt;
    private volatile ScheduledFuture<?> progressTask;

    public ModelService(LumoAiDetectorPlugin plugin, PluginSettings settings, MessageService messages, Platform platform, ModelRepository repository, RuntimeStateService runtimeStateService, ScheduledExecutorService timerExecutor, ExecutorService trainExecutor, StatsService statsService) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.platform = platform;
        this.repository = repository;
        this.runtimeStateService = runtimeStateService;
        this.timerExecutor = timerExecutor;
        this.trainExecutor = trainExecutor;
        this.statsService = statsService;
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public void loadStartupModel() {
        String runtimeModel = runtimeStateService.activeModel();
        String configured = runtimeModel == null || runtimeModel.trim().isEmpty() ? settings.configActiveModel : runtimeModel;
        if (configured == null || configured.trim().isEmpty()) {
            return;
        }
        ActivationResult result = activate(configured, true);
        if (result.status() == ActivationResult.Status.ERROR) {
            plugin.getLogger().warning("Cannot load model " + configured + ": " + result.error());
        }
    }

    public ActivationResult activate(String modelName, boolean force) {
        ModelInfo existing = activeInfo;
        if (!force && existing != null && !existing.name().equals(modelName)) {
            return ActivationResult.conflict(modelName, existing.name());
        }
        if (existing != null && existing.name().equals(modelName)) {
            return ActivationResult.alreadyActive(modelName);
        }
        try {
            ModelBundle bundle = repository.loadModel(modelName);
            if (bundle == null) {
                return ActivationResult.notFound(modelName);
            }
            activeModel = bundle.model();
            activeInfo = bundle.info();
            runtimeStateService.setActiveModel(bundle.info().name());
            return ActivationResult.activated(bundle.info().name());
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Cannot activate model " + modelName, exception);
            return ActivationResult.error(modelName, exception.getMessage());
        }
    }

    public void deactivate() {
        activeModel = null;
        activeInfo = null;
        runtimeStateService.setActiveModel("");
    }

    public void activateAsync(final CommandSender sender, final String modelName) {
        trainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ModelBundle bundle = repository.loadModel(modelName);
                    if (bundle == null) {
                        platform.send(sender, messages.get("model.not-found", messages.placeholders("model", modelName)));
                        return;
                    }
                    final String name = bundle.info().name();
                    activeModel = bundle.model();
                    activeInfo = bundle.info();
                    runtimeStateService.setActiveModel(name);
                    platform.send(sender, messages.get("model.activated", messages.placeholders("model", name)));
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Cannot activate model " + modelName, exception);
                    platform.send(sender, messages.get("model.error", messages.placeholders("model", modelName, "error", exception.getMessage())));
                }
            }
        });
    }

    public void train(final CommandSender sender) {
        if (!training.compareAndSet(false, true)) {
            platform.send(sender, messages.get("train.already", messages.placeholders("phase", trainingPhase, "elapsed", Formats.duration(System.currentTimeMillis() - trainingStartedAt))));
            return;
        }
        trainingStartedAt = System.currentTimeMillis();
        trainingPhase = "dataset";
        startProgressMessages();
        trainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                runTraining(sender);
            }
        });
    }

    public PredictionResult predict(double[] features) {
        RandomForest model = activeModel;
        ModelInfo info = activeInfo;
        if (model == null || info == null) {
            return PredictionResult.unavailable();
        }
        try {
            double[] posteriori = new double[2];
            int label = model.predict(features, posteriori);
            double confidence = label == 1 ? posteriori[Math.min(1, posteriori.length - 1)] * 100.0D : posteriori[0] * 100.0D;
            if (Double.isNaN(confidence) || Double.isInfinite(confidence) || confidence <= 0.0D) {
                confidence = label == 1 ? 100.0D : 0.0D;
            }
            return new PredictionResult(true, label, Math.max(0.0D, Math.min(100.0D, confidence)), info.name());
        } catch (Throwable throwable) {
            try {
                int label = model.predict(features);
                return new PredictionResult(true, label, label == 1 ? 100.0D : 0.0D, info.name());
            } catch (Throwable second) {
                plugin.getLogger().log(Level.WARNING, "Prediction failed", second);
                return PredictionResult.unavailable();
            }
        }
    }

    public boolean isTraining() {
        return training.get();
    }

    public String trainingPhase() {
        return trainingPhase;
    }

    public long trainingElapsedMillis() {
        return trainingStartedAt == 0L ? 0L : System.currentTimeMillis() - trainingStartedAt;
    }

    public String activeModelName() {
        ModelInfo info = activeInfo;
        return info == null ? "" : info.name();
    }

    public ModelInfo activeInfo() {
        return activeInfo;
    }

    private void runTraining(CommandSender sender) {
        try {
            plugin.datasetService().flushPending();
            DatasetSnapshot snapshot = plugin.datasetService().snapshot();
            int totalRows = snapshot.rows();
            if (totalRows > 50000) {
                platform.broadcastPermission("LumoAiDetector.train", "Dataset has " + totalRows + " rows. Training may take a while and use significant RAM.");
            }
            if (totalRows < settings.minTotalRows || snapshot.legitRows() < settings.minLegitRows || snapshot.cheaterRows() < settings.minCheaterRows) {
                failTraining("not-enough-data");
                platform.send(sender, messages.get("train.not-enough-data", messages.placeholders("rows", totalRows, "legit", snapshot.legitRows(), "cheater", snapshot.cheaterRows())));
                return;
            }
            platform.send(sender, messages.get("train.started", messages.placeholders("rows", totalRows)));
            trainingPhase = "split";
            Split split = split(snapshot);
            trainingPhase = "random-forest";
            int randomFeatures = settings.randomFeatures > 0 ? settings.randomFeatures : Math.max(1, (int) Math.floor(Math.sqrt(DatasetCsv.FEATURE_COUNT)));
            int adaptiveMaxNodes = settings.maxNodes;
            int adaptiveNodeSize = settings.nodeSize;
            int legitRows = snapshot.legitRows();
            int cheaterRows = snapshot.cheaterRows();
            if (totalRows < 500) {
                adaptiveMaxNodes = Math.min(adaptiveMaxNodes, 40);
                adaptiveNodeSize = Math.max(adaptiveNodeSize, 10);
            } else if (totalRows < 2000) {
                adaptiveMaxNodes = Math.min(adaptiveMaxNodes, 100);
                adaptiveNodeSize = Math.max(adaptiveNodeSize, 5);
            } else {
                adaptiveMaxNodes = Math.min(Math.max(adaptiveMaxNodes, totalRows / 20), 500);
                adaptiveNodeSize = Math.max(3, adaptiveNodeSize);
            }
            if (legitRows > 0 && cheaterRows > 0) {
                double ratio = Math.max(legitRows, cheaterRows) / (double) Math.min(legitRows, cheaterRows);
                if (ratio > 4.0) {
                    adaptiveNodeSize = Math.max(adaptiveNodeSize, 8);
                    if (settings.maxNodes <= 100) {
                        adaptiveMaxNodes = Math.min(adaptiveMaxNodes, 60);
                    }
                }
            }
            RandomForest.Trainer trainer = new RandomForest.Trainer(settings.randomForestTrees, randomFeatures);
            trainer.setNumRandomFeatures(randomFeatures);
            trainer.setMaxNodes(adaptiveMaxNodes);
            trainer.setNodeSize(adaptiveNodeSize);
            long started = System.currentTimeMillis();
            RandomForest model = trainer.train(split.trainX, split.trainY);
            trainingPhase = "metrics";
            Metrics metrics = metrics(model, split.metricX, split.metricY);
            long trainingMillis = System.currentTimeMillis() - started;
            String name = "model-" + Formats.fileDate(System.currentTimeMillis());
            ModelMetadata metadata = new ModelMetadata(name, name + ".bin", System.currentTimeMillis(), trainingMillis, totalRows, snapshot.legitRows(), snapshot.cheaterRows(), split.metricY.length, metrics.accuracy, metrics.precision, metrics.recall, metrics.f1, metrics.falsePositiveRate, model.size(), DatasetCsv.FEATURE_COUNT, plugin.getDescription().getVersion(), platform.serverName());
            trainingPhase = "saving";
            repository.saveModel(model, metadata);
            if (settings.autoActivateAfterTraining) {
                activeModel = model;
                activeInfo = repository.findModel(name);
                runtimeStateService.setActiveModel(name);
            }
            statsService.save();
            String success = messages.get("train.success", messages.placeholders("model", name, "time", Formats.duration(trainingMillis), "accuracy", Formats.percent(metrics.accuracy), "precision", Formats.percent(metrics.precision), "recall", Formats.percent(metrics.recall), "f1", Formats.percent(metrics.f1)));
            platform.broadcastPermission("LumoAiDetector.train", success);
            trainingPhase = "idle";
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Training failed", throwable);
            failTraining(throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage());
        } finally {
            stopProgressMessages();
            training.set(false);
        }
    }

    private void failTraining(String reason) {
        platform.broadcastPermission("LumoAiDetector.train", messages.get("train.failed", messages.placeholders("reason", reason)));
        trainingPhase = "idle";
    }

    private void startProgressMessages() {
        stopProgressMessages();
        progressTask = timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!training.get()) {
                    return;
                }
                platform.broadcastPermission("LumoAiDetector.train", messages.get("train.progress", messages.placeholders("phase", trainingPhase, "elapsed", Formats.duration(System.currentTimeMillis() - trainingStartedAt))));
            }
        }, settings.progressIntervalSeconds, settings.progressIntervalSeconds, TimeUnit.SECONDS);
    }

    private void stopProgressMessages() {
        if (progressTask != null) {
            progressTask.cancel(false);
            progressTask = null;
        }
    }

    private Split split(DatasetSnapshot snapshot) {
        int rows = snapshot.rows();
        int[] y = snapshot.y();
        List<Integer> legitIdx = new ArrayList<Integer>();
        List<Integer> cheatIdx = new ArrayList<Integer>();
        for (int i = 0; i < rows; i++) {
            if (y[i] == 0) {
                legitIdx.add(Integer.valueOf(i));
            } else {
                cheatIdx.add(Integer.valueOf(i));
            }
        }
        int validation = rows * settings.validationPercent / 100;
        if (validation <= 0 || rows - validation < 2 || legitIdx.size() < 2 || cheatIdx.size() < 2) {
            validation = 0;
        }
        Collections.shuffle(legitIdx, new Random(System.nanoTime()));
        Collections.shuffle(cheatIdx, new Random(System.nanoTime()));
        double[][] x = snapshot.x();
        if (validation == 0) {
            double[][] trainX = new double[rows][DatasetCsv.FEATURE_COUNT];
            int[] trainY = new int[rows];
            for (int i = 0; i < rows; i++) {
                trainX[i] = x[i];
                trainY[i] = y[i];
            }
            return new Split(trainX, trainY, trainX, trainY);
        }
        int legitVal = Math.max(1, legitIdx.size() * settings.validationPercent / 100);
        int cheatVal = Math.max(1, cheatIdx.size() * settings.validationPercent / 100);
        legitVal = Math.min(legitVal, legitIdx.size() - 1);
        cheatVal = Math.min(cheatVal, cheatIdx.size() - 1);
        int trainRows = rows - legitVal - cheatVal;
        double[][] trainX = new double[trainRows][DatasetCsv.FEATURE_COUNT];
        int[] trainY = new int[trainRows];
        double[][] metricX = new double[legitVal + cheatVal][DatasetCsv.FEATURE_COUNT];
        int[] metricY = new int[legitVal + cheatVal];
        int t = 0;
        for (int i = legitVal; i < legitIdx.size(); i++) {
            int src = legitIdx.get(i).intValue();
            trainX[t] = x[src];
            trainY[t] = y[src];
            t++;
        }
        for (int i = cheatVal; i < cheatIdx.size(); i++) {
            int src = cheatIdx.get(i).intValue();
            trainX[t] = x[src];
            trainY[t] = y[src];
            t++;
        }
        int m = 0;
        for (int i = 0; i < legitVal; i++) {
            int src = legitIdx.get(i).intValue();
            metricX[m] = x[src];
            metricY[m] = y[src];
            m++;
        }
        for (int i = 0; i < cheatVal; i++) {
            int src = cheatIdx.get(i).intValue();
            metricX[m] = x[src];
            metricY[m] = y[src];
            m++;
        }
        return new Split(trainX, trainY, metricX, metricY);
    }

    private Metrics metrics(RandomForest model, double[][] x, int[] y) {
        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;
        for (int i = 0; i < y.length; i++) {
            int predicted = model.predict(x[i]);
            if (predicted == 1 && y[i] == 1) {
                tp++;
            } else if (predicted == 0 && y[i] == 0) {
                tn++;
            } else if (predicted == 1) {
                fp++;
            } else {
                fn++;
            }
        }
        double total = Math.max(1, y.length);
        double accuracy = (tp + tn) * 100.0D / total;
        double precision = tp + fp == 0 ? 0.0D : tp * 100.0D / (tp + fp);
        double recall = tp + fn == 0 ? 0.0D : tp * 100.0D / (tp + fn);
        double f1 = precision + recall == 0 ? 0.0D : 2.0D * precision * recall / (precision + recall);
        double falsePositiveRate = fp + tn == 0 ? 0.0D : fp * 100.0D / (fp + tn);
        return new Metrics(accuracy, precision, recall, f1, falsePositiveRate);
    }

    private static final class Split {
        private final double[][] trainX;
        private final int[] trainY;
        private final double[][] metricX;
        private final int[] metricY;

        private Split(double[][] trainX, int[] trainY, double[][] metricX, int[] metricY) {
            this.trainX = trainX;
            this.trainY = trainY;
            this.metricX = metricX;
            this.metricY = metricY;
        }
    }

    private static final class Metrics {
        private final double accuracy;
        private final double precision;
        private final double recall;
        private final double f1;
        private final double falsePositiveRate;

        private Metrics(double accuracy, double precision, double recall, double f1, double falsePositiveRate) {
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.f1 = f1;
            this.falsePositiveRate = falsePositiveRate;
        }
    }
}
