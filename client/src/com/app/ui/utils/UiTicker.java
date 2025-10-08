package com.app.ui.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Labeled;
import javafx.util.Duration;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Simple global JavaFX Timeline ticker that runs every second on the FX thread
 * and invokes registered tasks. Controllers can register/unregister tasks
 * in their lifecycle methods to update UI elements periodically.
 */
public final class UiTicker {
    private static final Duration DEFAULT_PERIOD = Duration.seconds(1);
    private static final UiTicker INSTANCE = new UiTicker();

    private final Map<String, Runnable> taskIdToTask = new ConcurrentHashMap<>();
    private Timeline timeline;

    private UiTicker() {}

    public static UiTicker getInstance() {
        return INSTANCE;
    }

    /** Starts the ticker if not already running. */
    public synchronized void start() {
        if (timeline != null) {
            return;
        }
        timeline = new Timeline(new KeyFrame(DEFAULT_PERIOD, e -> runAll()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /** Stops the ticker and clears tasks. */
    public synchronized void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        taskIdToTask.clear();
    }

    /** Registers or replaces a periodic task (runs on FX thread). */
    public void registerTask(String id, Runnable task) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(task, "task");
        taskIdToTask.put(id, task);
        start();
    }

    /** Convenience: periodically set text on a Labeled using a Supplier. */
    public void registerLabelBinding(String id, Labeled labeled, Supplier<String> supplier) {
        Objects.requireNonNull(labeled, "labeled");
        Objects.requireNonNull(supplier, "supplier");
        WeakReference<Labeled> ref = new WeakReference<>(labeled);
        registerTask(id, () -> {
            Labeled l = ref.get();
            if (l == null) {
                // Auto-unregister if the control was GC'd
                unregister(id);
                return;
            }
            String value = supplier.get();
            if (value != null) {
                l.setText(value);
            }
        });
    }

    /** Unregisters a task by id. */
    public void unregister(String id) {
        if (id != null) taskIdToTask.remove(id);
    }

    private void runAll() {
        for (Runnable r : taskIdToTask.values()) {
            try {
                r.run();
            } catch (Throwable t) {
                // swallow to keep ticker alive; consider logging
            }
        }
    }
}


