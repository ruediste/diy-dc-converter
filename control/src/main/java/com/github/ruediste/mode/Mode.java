package com.github.ruediste.mode;

import java.awt.Component;
import java.io.Serializable;

public abstract class Mode<TSettings extends Serializable> {
    public String name;

    public Mode(String name) {
        this.name = name;
    }

    public abstract static class ModeInstance<TSettings extends Serializable> {
        public abstract Component initialize(TSettings settings, Runnable onChange);

        public abstract Object toConfigMessage(TSettings settings);
    }

    public abstract TSettings createDefaultSettings();

    public abstract ModeInstance<TSettings> createInstance();
}
