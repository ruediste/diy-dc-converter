package com.github.ruediste;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Throwable;
}
