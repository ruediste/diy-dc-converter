package com.github.ruediste.mode;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.*;

import com.github.ruediste.InterfaceMessage;
import com.github.ruediste.Datatype;

public class SimpleControlMode extends Mode<SimpleControlMode.Settings> {

    public SimpleControlMode() {
        super("Simple Control");
    }

    public static class SimpleControlStatusMessage implements InterfaceMessage {
        @Datatype.uint16
        public int compareValue;

        public float duty;
    }

    public static class SimpleControlConfigMessage implements InterfaceMessage {
        @Datatype.uint16
        public int pwmReload;

        @Datatype.uint16
        public int pwmPrescale;

        @Datatype.uint16
        public int pwmMaxCompare;

        @Datatype.uint16
        public int ctrlReload;

        @Datatype.uint16
        public int ctrlPrescale;

        @Datatype.uint16
        public int targetAdc;

        public float dutyChangeStep;

        public boolean running;

        public byte adcSampleCycles;
    }

    public static class Settings implements Serializable {
        /** in hertz */
        double frequencyPwm = 10e3;

        /** in hertz */
        double frequencyControl = 10e3;

        int targetAdc = 1000;

        double maxDuty = 0.01;
        double dutyChangeStep = 0.01;

        boolean running;

        protected int adcSampleCycles;
    }

    @Override
    public ModeInstance<SimpleControlMode.Settings> createInstance() {
        return new ModeInstance<SimpleControlMode.Settings>() {

            JLabel statusLabel = new JLabel();

            @Override
            public Component initializeImpl(Settings settings, Runnable onChange) {
                JPanel main = new JPanel(new GridLayout(8, 2));
                main.add(new JLabel("Control Frequency [kHz]"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel(settings.frequencyControl / 1e3, 1e-3, 500, 1e-3)),
                                value -> settings.frequencyControl = value * 1e3));

                main.add(new JLabel("PWM Frequency [kHz]"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel(settings.frequencyPwm / 1e3, 1e-3, 500, 1e-3)),
                                value -> settings.frequencyPwm = value * 1e3));

                main.add(new JLabel("ADC Sampling Cycles"));
                main.add(ui.registerAdcCycles(i -> settings.adcSampleCycles = i));

                main.add(new JLabel("Target ADC"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel((float) settings.targetAdc, 0, 4095, 1)),
                                value -> settings.targetAdc = (int) Math.round(value)));

                main.add(new JLabel("Max Duty [%]"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel(settings.maxDuty * 100, 0, 100, 0.1)),
                                value -> settings.maxDuty = value / 100));

                main.add(new JLabel("Duty Change Step [%]"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel(settings.dutyChangeStep * 100, 0, 100, 1e-3)),
                                value -> settings.dutyChangeStep = value / 100));

                main.add(new JLabel("Running"));
                var running = new JCheckBox();
                running.setSelected(settings.running);
                running.addActionListener(e -> {
                    settings.running = running.isSelected();
                    onChange.run();
                });
                main.add(running);

                main.add(new JLabel("Status"));
                main.add(statusLabel);

                return main;
            }

            @Override
            public void handle(Object msg, Settings settings) {
                if (msg instanceof SimpleControlStatusMessage status) {
                    statusLabel.setText("Compare Value: " + status.compareValue + " Duty [%]: "
                            + String.format("%.1f", status.duty * 100));
                }

            }

            @Override
            public Object toConfigMessage(Settings settings) {
                var calc = new PwmValuesCalculator();
                var msg = new SimpleControlConfigMessage();
                {
                    var controlPwm = calc.calculate(settings.frequencyControl, 0);
                    msg.ctrlReload = (int) controlPwm.reload;
                    msg.ctrlPrescale = (int) controlPwm.prescale;
                }
                {
                    var pwm = calc.calculate(settings.frequencyPwm, settings.maxDuty);
                    msg.pwmReload = (int) pwm.reload;
                    msg.pwmPrescale = (int) pwm.prescale;
                    msg.pwmMaxCompare = (int) pwm.compare;
                }
                msg.targetAdc = settings.targetAdc;
                msg.dutyChangeStep = (float) settings.dutyChangeStep;
                msg.adcSampleCycles = (byte) settings.adcSampleCycles;
                msg.running = settings.running;
                return msg;
            }

        };
    }

    @Override
    public Settings createDefaultSettings() {
        return new Settings();
    }

}
