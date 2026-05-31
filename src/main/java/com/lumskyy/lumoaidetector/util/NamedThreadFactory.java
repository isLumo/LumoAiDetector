package com.lumskyy.lumoaidetector.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NamedThreadFactory implements ThreadFactory {
    private final String name;
    private final AtomicInteger counter = new AtomicInteger();

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, name + "-" + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }
}
