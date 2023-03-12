package com.github.ruediste.mode;

import java.awt.Component;
import java.io.Serializable;

import javax.swing.JLabel;

public class DcmBoostPidControlMode extends Mode<DcmBoostPidControlMode.Settings> {

    public DcmBoostPidControlMode() {
        super("DCM Boost PID Control");
    }

    public static class Settings implements Serializable {

    }

    @Override
    public ModeInstance<DcmBoostPidControlMode.Settings> createInstance() {
        return new ModeInstance<DcmBoostPidControlMode.Settings>() {

            @Override
            public Component initialize(Settings settings, Runnable onChange) {
                return new JLabel("Foo");
            }

            @Override
            public Object toConfigMessage(Settings settings) {
                return null;
            }

        };
    }

    @Override
    public Settings createDefaultSettings() {
        return new Settings();
    }

}
