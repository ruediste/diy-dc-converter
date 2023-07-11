package com.github.ruediste.mode;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.PrintWriter;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import com.github.ruediste.InterfaceMessage;
import com.google.common.base.Charsets;
import com.github.ruediste.BlobMessage;

public class CotControlMode extends Mode<CotControlMode.Settings> {
    private final Logger log = LoggerFactory.getLogger(CotControlMode.class);

    public CotControlMode() {
        super("COT Control Mode");
    }

    public static class CotControlStatusMessage implements InterfaceMessage {
        public float integral;
        public float vOut;
        public boolean isInHystericMode;
    }

    public static class CotControlConfigMessage implements InterfaceMessage, Serializable {
        public float kP = 1e-5f;
        public float kI = 1e-5f;
        public float kD = 1e-5f;

        public float peakCurrent = 60e-3f;
        public float inductance = 3e-3f;
        public float controlFrequency = 10e3f;
        public float idleFraction = 0.1f;
        public float startupVoltageFactor = 1.1f;
        public float maxAdcVoltage = 20;
        public float targetVoltage = 12;

        public boolean running;
    }

    public static class Settings implements Serializable {
        public CotControlConfigMessage config = new CotControlConfigMessage();
    }

    @Override
    public ModeInstance<CotControlMode.Settings> createInstance() {
        return new ModeInstance<CotControlMode.Settings>() {

            JLabel statusLabel = new JLabel();

            @Override
            public Component initializeImpl(Settings settings, Runnable onChange) {
                JPanel main = new JPanel(new GridLayout(0, 2));

                main.add(new JLabel("kP"));
                main.add(ui.registerScientificF(settings.config.kP, x -> settings.config.kP = x));
                main.add(new JLabel("kI"));
                main.add(ui.registerScientificF(settings.config.kI, x -> settings.config.kI = x));
                main.add(new JLabel("kD"));
                main.add(ui.registerScientificF(settings.config.kD, x -> settings.config.kD = x));

                main.add(new JLabel("Peak Current [A]"));
                main.add(ui.registerScientificF(settings.config.peakCurrent, x -> settings.config.peakCurrent = x));
                main.add(new JLabel("Inductance [F]"));
                main.add(ui.registerScientificF(settings.config.inductance, x -> settings.config.inductance = x));
                main.add(new JLabel("Control Frequency [Hz]"));
                main.add(ui.registerScientificF(settings.config.controlFrequency,
                        x -> settings.config.controlFrequency = x));
                main.add(new JLabel("Idle Fraction [1]"));
                main.add(ui.registerScientificF(settings.config.idleFraction, x -> settings.config.idleFraction = x));
                main.add(new JLabel("Startup Voltage Factor [V]"));
                main.add(ui.registerScientificF(settings.config.startupVoltageFactor,
                        x -> settings.config.startupVoltageFactor = x));
                main.add(new JLabel("Max ADC Voltage [V]"));
                main.add(ui.registerScientificF(settings.config.maxAdcVoltage, x -> settings.config.maxAdcVoltage = x));
                main.add(new JLabel("Target Voltage [V]"));
                main.add(ui.registerScientificF(settings.config.targetVoltage, x -> settings.config.targetVoltage = x));

                main.add(new JLabel("Running"));
                var running = new JCheckBox();
                running.setSelected(settings.config.running);
                running.addActionListener(e -> {
                    settings.config.running = running.isSelected();
                    onChange.run();
                });
                main.add(running);

                main.add(new JLabel("Status"));
                main.add(statusLabel);

                return main;
            }

            @Override
            public void handle(Object msg, Settings settings) {
                if (msg instanceof CotControlStatusMessage status) {
                    statusLabel.setText("Hysteric Mode: " + status.isInHystericMode + " integral: " + status.integral
                            + " vOUt: " + status.vOut);
                }

                if (msg instanceof BlobMessage blob) {
                    try (var writer = new PrintWriter("data.csv", Charsets.UTF_8)) {
                        writer.println("adc, compare");
                        for (int i = 0; i + 6 <= blob.data.length;) {
                            int adc = blob.readUint16(i);
                            i += 2;
                            long integral = blob.readUint32(i);
                            i += 4;
                            writer.println(adc + "," + integral);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    log.info("Blob received");
                }

            }

            @Override
            public InterfaceMessage toConfigMessage(Settings settings) {
                return settings.config;
            }

        };
    }

    @Override
    public Settings createDefaultSettings() {
        return new Settings();
    }

}
