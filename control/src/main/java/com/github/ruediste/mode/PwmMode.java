package com.github.ruediste.mode;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GridLayout;
import java.io.PrintWriter;
import java.io.Serializable;

import com.github.ruediste.BlobMessage;
import com.github.ruediste.Datatype;
import com.github.ruediste.InterfaceMessage;
import com.google.common.base.Charsets;
import com.google.common.math.StatsAccumulator;

public class PwmMode extends Mode<PwmMode.Settings> {
    private final Logger log = LoggerFactory.getLogger(PwmMode.class);

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
        public int ctrlReload;

        @Datatype.uint16
        public int ctrlPrescale;

        @Datatype.uint16
        public int adcTrigger;

        public boolean running;

        public byte adcSampleCycles;
    }

    public static class Settings implements Serializable {
        /** in hertz */
        double frequency = 10e3;

        /** in hertz */
        double frequencyControl = 10e3;

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

                JPanel main = new JPanel(new GridLayout(0, 2));
                main.add(new JLabel("Frequency [kHz]"));
                main.add(register(new JSpinner(new SpinnerNumberModel(settings.frequency / 1e3, 1e-3, 500, 1e-3)),
                        value -> settings.frequency = value * 1e3));

                main.add(new JLabel("Control Frequency [kHz]"));
                main.add(
                        register(new JSpinner(new SpinnerNumberModel(settings.frequencyControl / 1e3, 1e-3, 500, 1e-3)),
                                value -> settings.frequencyControl = value * 1e3));

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
            public void handle(Object msg, Settings settings) {
                if (msg instanceof BlobMessage blob) {
                    var stats = new StatsAccumulator();
                    try (var writer = new PrintWriter("dataPwm.csv", Charsets.UTF_8)) {
                        writer.println("adc");
                        for (int i = 0; i + 2 <= blob.data.length;) {
                            int adc = blob.readUint16(i);
                            i += 2;
                            writer.println(adc);
                            stats.add(adc);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    log.info(
                            "Blob received. ADC mean: " + stats.mean() + " stdDev: " + stats.sampleStandardDeviation());
                }

            }

            @Override
            public InterfaceMessage toConfigMessage(Settings settings) {
                var msg = new PWMModeConfigMessage();
                var calc = new PwmValuesCalculator();
                {
                    var values = calc.calculate(settings.frequency, settings.duty);
                    msg.prescale = (int) values.prescale;
                    msg.reload = (int) values.reload;
                    msg.compare = (int) values.compare;
                    msg.adcTrigger = (int) (values.reload * settings.adcTriggerDuty);
                }

                {
                    var controlPwm = calc.calculate(settings.frequencyControl, 0);
                    msg.ctrlReload = (int) controlPwm.reload;
                    msg.ctrlPrescale = (int) controlPwm.prescale;
                }

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
