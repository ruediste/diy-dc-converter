package com.github.ruediste.mode;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.*;

import com.github.ruediste.InterfaceMessage;
import com.github.ruediste.serial.RealSerialConnection;
import com.github.ruediste.Datatype;
import com.github.ruediste.DebugMessage;

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
        public int reloadPwm;

        @Datatype.uint16
        public int prescalePwm;

        @Datatype.uint16
        public int reloadCtrl;

        @Datatype.uint16
        public int prescaleCtrl;

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
            JLabel[] debugLabels = new JLabel[4];

            @Override
            public Component initializeImpl(Settings settings, Runnable onChange) {
                JPanel main = new JPanel(new GridLayout(4 + debugLabels.length, 2));
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

                for (int i = 0; i < debugLabels.length; i++) {
                    main.add(new JLabel("Debug " + i));
                    debugLabels[i] = new JLabel();
                    main.add(debugLabels[i]);
                }

                return main;
            }

            @Override
            public void handle(Object msg) {
                if (msg instanceof DcmBoostPidStatusMessage status) {
                    statusLabel.setText("adc0: " + status.adc0 + " adc1: " + status.adc1);
                }

                if (msg instanceof DebugMessage dbg) {
                    for (int i = 0; i < debugLabels.length; i++) {
                        debugLabels[i].setText(RealSerialConnection.hexDump(dbg.data, i * 4, 4));
                    }
                }
            }

            @Override
            public Object toConfigMessage(Settings settings) {
                var calc = new PwmValuesCalculator();
                var msg = new DcmBoostPidConfigMessage();
                {
                    var controlPwm = calc.calculate(settings.frequencyControl, 0);
                    msg.reloadCtrl = (int) controlPwm.reload;
                    msg.prescaleCtrl = (int) controlPwm.prescale;
                }
                {
                    var pwm = calc.calculate(settings.frequencyPwm, 0);
                    msg.reloadPwm = (int) pwm.reload;
                    msg.prescalePwm = (int) pwm.prescale;
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
