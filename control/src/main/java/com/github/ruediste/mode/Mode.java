package com.github.ruediste.mode;

import java.awt.Component;
import java.io.Serializable;
import java.util.function.Consumer;

import javax.swing.JComboBox;
import javax.swing.JSpinner;

import com.github.ruediste.Listeners;

public abstract class Mode<TSettings extends Serializable> {
    public String name;

    public Mode(String name) {
        this.name = name;
    }

    public abstract static class ModeInstance<TSettings extends Serializable> {
        protected Listeners inputChanged = new Listeners();
        protected ModeUiUtil ui = new ModeUiUtil(inputChanged);

        public final Component initialize(TSettings settings, Runnable onChange) {
            inputChanged.register(onChange);
            return this.initializeImpl(settings, onChange);
        }

        public abstract Component initializeImpl(TSettings settings, Runnable onChange);

        public abstract Object toConfigMessage(TSettings settings);

        protected JSpinner register(JSpinner spinner, Consumer<Double> setter) {
            return ui.register(spinner, setter);
        }

        public void handle(Object msg, TSettings settings) {
        }
    }

    public static class ModeUiUtil {
        private Listeners inputChanged;

        ModeUiUtil(Listeners inputChanged) {
            this.inputChanged = inputChanged;
        }

        public JSpinner register(JSpinner spinner, Consumer<Double> setter) {
            spinner.addChangeListener(e -> {
                setter.accept((Double) spinner.getValue());
                inputChanged.trigger();
            });
            return spinner;
        }

        public JComboBox<String> registerAdcCycles(Consumer<Integer> setter) {
            var labels = new String[] { "3", "15", "28", "56", "84", "112", "144", "480" };
            var box = new JComboBox<String>(labels);
            box.addActionListener(e -> {
                setter.accept(box.getSelectedIndex());
                inputChanged.trigger();
            });
            return box;
        }

    }

    public abstract TSettings createDefaultSettings();

    public abstract ModeInstance<TSettings> createInstance();
}
