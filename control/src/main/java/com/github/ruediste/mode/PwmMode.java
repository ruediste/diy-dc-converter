package com.github.ruediste.mode;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridLayout;
import java.io.Serializable;

import com.github.ruediste.Datatype;
import com.github.ruediste.InterfaceMessage;

public class PwmMode extends Mode<PwmMode.Settings> {

    public PwmMode() {
        super("PWM");
    }

    public static class PWMModeConfigMessage implements InterfaceMessage {
        @Datatype.uint16
        public int prescale;

        @Datatype.uint16
        public int reload;

        @Datatype.uint16
        public int compare;

        @Datatype.uint16
        public int adcTrigger;

        public boolean running;

        public byte adcSampleCycles;
    }

    public static class Settings implements Serializable {
        /** in hertz */
        double frequency = 10e3;

        /** 0..1 */
        double duty = 0.1;

        /** 0..1 */
        double adcTriggerDuty = 0.1;

        boolean running;

        int adcSampleCycles;
    }

    @Override
    public ModeInstance<PwmMode.Settings> createInstance() {
        return new ModeInstance<PwmMode.Settings>() {

            @Override
            public Component initializeImpl(Settings settings, Runnable onChange) {

                JPanel main = new JPanel(new GridLayout(6, 2));
                main.add(new JLabel("Frequency [kHz]"));
                main.add(register(new JSpinner(new SpinnerNumberModel(settings.frequency / 1e3, 1e-3, 500, 1e-3)),
                        value -> settings.frequency = value * 1e3));

                main.add(new JLabel("Duty [%]"));
                main.add(register(new JSpinner(new SpinnerNumberModel(settings.duty * 100, 0, 100, 1)),
                        value -> settings.duty = value / 100));

                main.add(new JLabel("ADC Trigger [%]"));
                main.add(register(new JSpinner(new SpinnerNumberModel(settings.adcTriggerDuty * 100, 0, 100, 1)),
                        value -> settings.adcTriggerDuty = value / 100));

                main.add(new JLabel("ADC Sampling Cycles"));
                main.add(ui.registerAdcCycles(i -> settings.adcSampleCycles = i));

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
                msg.adcTrigger = (int) (values.reload * settings.adcTriggerDuty);
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
