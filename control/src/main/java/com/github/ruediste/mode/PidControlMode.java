package com.github.ruediste.mode;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.*;

import com.github.ruediste.InterfaceMessage;
import com.github.ruediste.Datatype;

public class PidControlMode extends Mode<PidControlMode.Settings> {

    public PidControlMode() {
        super("PID Control Mode");
    }

    public static class PidControlStatusMessage implements InterfaceMessage {
        @Datatype.uint16
        public int compareValue;

        public float duty;
    }

    public static class PidControlConfigMessage implements InterfaceMessage {
        @Datatype.uint16
        public int pwmReload;

        @Datatype.uint16
        public int pwmPrescale;

        @Datatype.uint16
        public int ctrlReload;

        @Datatype.uint16
        public int ctrlPrescale;

        @Datatype.uint16
        public int targetAdc;

        public float kP;
        public float kI;
        public float kD;
        public float maxDuty;

        @Datatype.int32
        public int maxIntegral;

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
        double kP;
        double kI;
        double kD;

        boolean running;

        protected int adcSampleCycles;
    }

    @Override
    public ModeInstance<PidControlMode.Settings> createInstance() {
        return new ModeInstance<PidControlMode.Settings>() {

            JLabel statusLabel = new JLabel();

            @Override
            public Component initializeImpl(Settings settings, Runnable onChange) {
                JPanel main = new JPanel(new GridLayout(0, 2));
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

                main.add(new JLabel("kP"));
                main.add(ui.registerScientific(settings.kP, x -> settings.kP = x));
                main.add(new JLabel("kI"));
                main.add(ui.registerScientific(settings.kI, x -> settings.kI = x));
                main.add(new JLabel("kD"));
                main.add(ui.registerScientific(settings.kD, x -> settings.kD = x));

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
                if (msg instanceof PidControlStatusMessage status) {
                    statusLabel.setText("compare: " + status.compareValue + " duty: " + status.duty);
                }

            }

            @Override
            public Object toConfigMessage(Settings settings) {
                var calc = new PwmValuesCalculator();
                var msg = new PidControlConfigMessage();
                {
                    var controlPwm = calc.calculate(settings.frequencyControl, 0);
                    msg.ctrlReload = (int) controlPwm.reload;
                    msg.ctrlPrescale = (int) controlPwm.prescale;
                }
                {
                    var pwm = calc.calculate(settings.frequencyPwm, settings.maxDuty);
                    msg.pwmReload = (int) pwm.reload;
                    msg.pwmPrescale = (int) pwm.prescale;
                }
                msg.targetAdc = settings.targetAdc;
                msg.kP = (float) settings.kP;
                msg.kI = (float) settings.kI;
                msg.kD = (float) settings.kD;
                msg.maxDuty = (float) settings.maxDuty;

                if (settings.kI == 0 || 1 / settings.kI > Integer.MAX_VALUE)
                    msg.maxIntegral = Integer.MAX_VALUE;
                else
                    msg.maxIntegral = (int) (1 / settings.kI);

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
