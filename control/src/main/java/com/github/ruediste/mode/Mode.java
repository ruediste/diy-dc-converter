package com.github.ruediste.mode;

import java.awt.Component;
import java.io.Serializable;
import java.util.function.Consumer;

import javax.swing.JSpinner;

import com.github.ruediste.Listeners;

public abstract class Mode<TSettings extends Serializable> {
    public String name;

    public Mode(String name) {
        this.name = name;
    }

    public abstract static class ModeInstance<TSettings extends Serializable> {
        protected Listeners inputChanged = new Listeners();

        public final Component initialize(TSettings settings, Runnable onChange) {
            inputChanged.register(onChange);
            return this.initializeImpl(settings, onChange);
        }

        public abstract Component initializeImpl(TSettings settings, Runnable onChange);

        public abstract Object toConfigMessage(TSettings settings);

        protected JSpinner register(JSpinner spinner, Consumer<Double> setter) {
            spinner.addChangeListener(e -> {
                setter.accept((Double) spinner.getValue());
                inputChanged.trigger();
            });
            return spinner;
        }

        public void handle(Object msg) {
        }
    }

    public abstract TSettings createDefaultSettings();

    public abstract ModeInstance<TSettings> createInstance();
}
