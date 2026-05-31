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
    private final StatsService statsService;
    private volatile PluginSettings settings;
    private volatile RandomForest activeModel;
    private volatile ModelInfo activeInfo;
    private final AtomicBoolean training = new AtomicBoolean(false);
    private volatile String trainingPhase = "idle";
    private volatile long trainingStartedAt;
    private volatile ScheduledFuture<?> progressTask;

    public ModelService(LumoAiDetectorPlugin plugin, PluginSettings settings, MessageService messages, Platform platform, ModelRepository repository, RuntimeStateService runtimeStateService, ScheduledExecutorService timerExecutor, StatsService statsService) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.platform = platform;
        this.repository = repository;
        this.runtimeStateService = runtimeStateService;
        this.timerExecutor = timerExecutor;
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

    public void train(final CommandSender sender) {
        if (!training.compareAndSet(false, true)) {
            platform.send(sender, messages.get("train.already", messages.placeholders("phase", trainingPhase, "elapsed", Formats.duration(System.currentTimeMillis() - trainingStartedAt))));
            return;
        }
        trainingStartedAt = System.currentTimeMillis();
        trainingPhase = "dataset";
        startProgressMessages();
        timerExecutor.execute(new Runnable() {
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
            DatasetSnapshot snapshot = plugin.datasetService().snapshot();
            if (snapshot.rows() < settings.minTotalRows || snapshot.legitRows() < settings.minLegitRows || snapshot.cheaterRows() < settings.minCheaterRows) {
                failTraining("not-enough-data");
                platform.send(sender, messages.get("train.not-enough-data", messages.placeholders("rows", snapshot.rows(), "legit", snapshot.legitRows(), "cheater", snapshot.cheaterRows())));
                return;
            }
            platform.send(sender, messages.get("train.started", messages.placeholders("rows", snapshot.rows())));
            trainingPhase = "split";
            Split split = split(snapshot);
            trainingPhase = "random-forest";
            int randomFeatures = settings.randomFeatures > 0 ? settings.randomFeatures : Math.max(1, (int) Math.floor(Math.sqrt(DatasetCsv.FEATURE_COUNT)));
            RandomForest.Trainer trainer = new RandomForest.Trainer(settings.randomForestTrees, randomFeatures);
            trainer.setNumRandomFeatures(randomFeatures);
            trainer.setMaxNodes(settings.maxNodes);
            trainer.setNodeSize(settings.nodeSize);
            long started = System.currentTimeMillis();
            RandomForest model = trainer.train(split.trainX, split.trainY);
            trainingPhase = "metrics";
            Metrics metrics = metrics(model, split.metricX, split.metricY);
            long trainingMillis = System.currentTimeMillis() - started;
            String name = "model-" + Formats.fileDate(System.currentTimeMillis());
            ModelMetadata metadata = new ModelMetadata(name, name + ".bin", System.currentTimeMillis(), trainingMillis, snapshot.rows(), snapshot.legitRows(), snapshot.cheaterRows(), split.metricY.length, metrics.accuracy, metrics.precision, metrics.recall, metrics.falsePositiveRate, model.size(), DatasetCsv.FEATURE_COUNT, plugin.getDescription().getVersion(), platform.serverName());
            trainingPhase = "saving";
            repository.saveModel(model, metadata);
            if (settings.autoActivateAfterTraining) {
                activeModel = model;
                activeInfo = repository.findModel(name);
                runtimeStateService.setActiveModel(name);
            }
            statsService.save();
            String success = messages.get("train.success", messages.placeholders("model", name, "time", Formats.duration(trainingMillis), "accuracy", Formats.percent(metrics.accuracy), "precision", Formats.percent(metrics.precision), "recall", Formats.percent(metrics.recall)));
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
        int validation = rows * settings.validationPercent / 100;
        if (validation <= 0 || rows - validation < 2) {
            validation = 0;
        }
        List<Integer> indexes = new ArrayList<Integer>();
        for (int i = 0; i < rows; i++) {
            indexes.add(Integer.valueOf(i));
        }
        Collections.shuffle(indexes, new Random(System.nanoTime()));
        int trainRows = rows - validation;
        double[][] trainX = new double[trainRows][DatasetCsv.FEATURE_COUNT];
        int[] trainY = new int[trainRows];
        double[][] metricX = new double[validation == 0 ? rows : validation][DatasetCsv.FEATURE_COUNT];
        int[] metricY = new int[validation == 0 ? rows : validation];
        for (int i = 0; i < trainRows; i++) {
            int source = indexes.get(i).intValue();
            trainX[i] = snapshot.x()[source];
            trainY[i] = snapshot.y()[source];
        }
        if (validation == 0) {
            for (int i = 0; i < rows; i++) {
                metricX[i] = snapshot.x()[i];
                metricY[i] = snapshot.y()[i];
            }
        } else {
            for (int i = 0; i < validation; i++) {
                int source = indexes.get(trainRows + i).intValue();
                metricX[i] = snapshot.x()[source];
                metricY[i] = snapshot.y()[source];
            }
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
        double falsePositiveRate = fp + tn == 0 ? 0.0D : fp * 100.0D / (fp + tn);
        return new Metrics(accuracy, precision, recall, falsePositiveRate);
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
        private final double falsePositiveRate;

        private Metrics(double accuracy, double precision, double recall, double falsePositiveRate) {
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.falsePositiveRate = falsePositiveRate;
        }
    }
}
