package com.github.ruediste.mode;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridLayout;
import java.io.Serializable;
import java.util.function.Consumer;

import com.github.ruediste.Datatype;
import com.github.ruediste.InterfaceMessage;
import com.github.ruediste.UiRefresh;

public class PwmMode extends Mode<PwmMode.Settings> {

    public PwmMode() {
        super("PWM");
    }

    public static class PWMModeConfigMessage implements InterfaceMessage {
        @Datatype.uint16
        public int reload;

        @Datatype.uint16
        public int compare;

        @Datatype.uint16
        public int prescale;

        public boolean running;
    }

    public static class Settings implements Serializable {
        /** in hertz */
        double frequency = 10e3;

        /** 0..1 */
        double duty = 0.1;

        boolean running;
    }

    @Override
    public ModeInstance<PwmMode.Settings> createInstance() {
        return new ModeInstance<PwmMode.Settings>() {

            @Override
            public Component initializeImpl(Settings settings, Runnable onChange) {

                JPanel main = new JPanel(new GridLayout(4, 2));
                main.add(new JLabel("Frequency [kHz]"));
                main.add(register(new JSpinner(new SpinnerNumberModel(settings.frequency / 1e3, 1e-3, 500, 1e-3)),
                        value -> settings.frequency = value * 1e3));

                main.add(new JLabel("Duty [%]"));
                main.add(register(new JSpinner(new SpinnerNumberModel(settings.duty * 100, 0, 100, 1)),
                        value -> settings.duty = value / 100));

                main.add(new JLabel("Running"));
                var running = new JCheckBox();
                running.setSelected(settings.running);
                running.addActionListener(e -> {
                    settings.running = running.isSelected();
                    onChange.run();
                });

                main.add(running);

                return main;
            }

            @Override
            public Object toConfigMessage(Settings settings) {
                var msg = new PWMModeConfigMessage();
                var values = new PwmValuesCalculator().calculate(settings.frequency, settings.duty);
                msg.prescale = (int) values.prescale;
                msg.reload = (int) values.reload;
                msg.compare = (int) values.compare;
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
