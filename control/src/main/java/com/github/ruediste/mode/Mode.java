package com.github.ruediste.mode;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import com.github.ruediste.InterfaceMessage;
import com.github.ruediste.Listeners;

public abstract class Mode<TSettings extends Serializable> {
    public String name;

    public Mode(String name) {
        this.name = name;
    }

    public abstract static class ModeInstance<TSettings extends Serializable> {
        protected Listeners inputChanged = new Listeners();
        protected ModeUiUtil ui = new ModeUiUtil(inputChanged);

        public final Component initialize(TSettings settings, Runnable onChange) {
            inputChanged.register(onChange);
            return this.initializeImpl(settings, onChange);
        }

        public abstract Component initializeImpl(TSettings settings, Runnable onChange);

        public abstract InterfaceMessage toConfigMessage(TSettings settings);

        protected JSpinner register(JSpinner spinner, Consumer<Double> setter) {
            return ui.register(spinner, setter);
        }

        public void handle(Object msg, TSettings settings) {
        }
    }

    public static class ModeUiUtil {
        private Listeners inputChanged;

        ModeUiUtil(Listeners inputChanged) {
            this.inputChanged = inputChanged;
        }

        public JSpinner register(JSpinner spinner, Consumer<Double> setter) {
            spinner.addChangeListener(e -> {
                setter.accept((Double) spinner.getValue());
                inputChanged.trigger();
            });
            return spinner;
        }

        public JComboBox<String> registerAdcCycles(Consumer<Integer> setter) {
            var labels = new String[] { "3", "15", "28", "56", "84", "112", "144", "480" };
            var box = new JComboBox<String>(labels);
            box.addActionListener(e -> {
                setter.accept(box.getSelectedIndex());
                inputChanged.trigger();
            });
            return box;
        }

        public JTextField registerScientific(double initialValue, Consumer<Double> setter) {
            var field = new JTextField("%.3e".formatted(initialValue));
            addChangeListener(field, e -> {
                try {
                    setter.accept(Double.parseDouble(field.getText()));
                    field.setBackground(Color.white);
                    inputChanged.trigger();
                } catch (NumberFormatException ex) {
                    field.setBackground(Color.orange);
                }
            });
            return field;
        }

        /**
         * Installs a listener to receive notification when the text of any
         * {@code JTextComponent} is changed. Internally, it installs a
         * {@link DocumentListener} on the text component's {@link Document},
         * and a {@link PropertyChangeListener} on the text component to detect
         * if the {@code Document} itself is replaced.
         * 
         * @param text           any text component, such as a {@link JTextField}
         *                       or {@link JTextArea}
         * @param changeListener a listener to receive {@link ChangeEvent}s
         *                       when the text is changed; the source object for the
         *                       events
         *                       will be the text component
         * @throws NullPointerException if either parameter is null
         */
        public static void addChangeListener(JTextComponent text, ChangeListener changeListener) {
            Objects.requireNonNull(text);
            Objects.requireNonNull(changeListener);
            DocumentListener dl = new DocumentListener() {
                private int lastChange = 0, lastNotifiedChange = 0;

                @Override
                public void insertUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    lastChange++;
                    SwingUtilities.invokeLater(() -> {
                        if (lastNotifiedChange != lastChange) {
                            lastNotifiedChange = lastChange;
                            changeListener.stateChanged(new ChangeEvent(text));
                        }
                    });
                }
            };
            text.addPropertyChangeListener("document", (PropertyChangeEvent e) -> {
                Document d1 = (Document) e.getOldValue();
                Document d2 = (Document) e.getNewValue();
                if (d1 != null)
                    d1.removeDocumentListener(dl);
                if (d2 != null)
                    d2.addDocumentListener(dl);
                dl.changedUpdate(null);
            });
            Document d = text.getDocument();
            if (d != null)
                d.addDocumentListener(dl);
        }

    }

    public abstract TSettings createDefaultSettings();

    public abstract ModeInstance<TSettings> createInstance();
}
