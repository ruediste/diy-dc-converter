package com.github.ruediste.mode;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.*;

import com.github.ruediste.InterfaceMessage;
import com.github.ruediste.Datatype;

public class DcmBoostPidControlMode extends Mode<DcmBoostPidControlMode.Settings> {

    public DcmBoostPidControlMode() {
        super("DCM Boost PID Control");
    }

    public static class DcmBoostPidStatusMessage implements InterfaceMessage {
        @Datatype.uint16
        public int adc0;

        @Datatype.uint16
        public int adc1;
    }

    public static class DcmBoostPidConfigMessage implements InterfaceMessage {
        public float kP;
        public float kI;
        public float kD;

        @Datatype.uint16
        public int pwmReload;

        @Datatype.uint16
        public int pwmPrescale;

        @Datatype.uint16
        public int ctrlReload;

        @Datatype.uint16
        public int ctrlPrescale;

        public boolean running;
    }

    public static class Settings implements Serializable {
        /** in hertz */
        double frequencyPwm = 10e3;

        /** in hertz */
        double frequencyControl = 10e3;

        boolean running;
    }

    @Override
    public ModeInstance<DcmBoostPidControlMode.Settings> createInstance() {
        return new ModeInstance<DcmBoostPidControlMode.Settings>() {

            JLabel statusLabel = new JLabel();

            @Override
            public Component initializeImpl(Settings settings, Runnable onChange) {
                JPanel main = new JPanel(new GridLayout(4, 2));
                main.add(new JLabel("Control Frequency [kHz]"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel(settings.frequencyControl / 1e3, 1e-3, 500, 1e-3)),
                                value -> settings.frequencyControl = value * 1e3));

                main.add(new JLabel("PWM Frequency [kHz]"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel(settings.frequencyPwm / 1e3, 1e-3, 500, 1e-3)),
                                value -> settings.frequencyPwm = value * 1e3));

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
                if (msg instanceof DcmBoostPidStatusMessage status) {
                    statusLabel.setText("adc0: " + status.adc0 + " adc1: " + status.adc1);
                }

            }

            @Override
            public Object toConfigMessage(Settings settings) {
                var calc = new PwmValuesCalculator();
                var msg = new DcmBoostPidConfigMessage();
                {
                    var controlPwm = calc.calculate(settings.frequencyControl, 0);
                    msg.ctrlReload = (int) controlPwm.reload;
                    msg.ctrlPrescale = (int) controlPwm.prescale;
                }
                {
                    var pwm = calc.calculate(settings.frequencyPwm, 0);
                    msg.pwmReload = (int) pwm.reload;
                    msg.pwmPrescale = (int) pwm.prescale;
                }
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
