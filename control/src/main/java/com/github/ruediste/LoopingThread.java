package com.github.ruediste;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopingThread {
    private final static Logger log = LoggerFactory.getLogger(LoopingThread.class);

    private volatile boolean closing;
    private CountDownLatch closed = new CountDownLatch(1);

    private String name;
    private ThrowingRunnable run;

    private ThrowingRunnable onClosed;

    public LoopingThread(String name, ThrowingRunnable run) {
        this(name, run, null);
    }

    public LoopingThread(String name, ThrowingRunnable run, ThrowingRunnable onClosed) {
        this.name = name;
        this.run = run;
        this.onClosed = onClosed;
        new Thread(this::loop, name).start();
    }

    private void loop() {
        log.info("Starting " + name);
        try {
            while (true) {
                run.run();
                if (closing) {
                    break;
                }
            }
        } catch (Throwable t) {
            log.error("Error in " + name, t);
        }
        if (onClosed != null) {
            try {
                onClosed.run();
            } catch (Throwable t) {
                log.error("Error in onClosed in " + name, t);
            }
        }
        closed.countDown();
        log.info("Stopped " + name);
    }

    public void stop() {
        closing = true;
        try {
            closed.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException(e);
        }
    }
}
