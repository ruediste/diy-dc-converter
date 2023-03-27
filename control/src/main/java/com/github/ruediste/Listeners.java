package com.github.ruediste;

import java.util.HashSet;
import java.util.Set;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Listeners implements ActionListener {
    private Set<Runnable> listeners = new HashSet<>();

    public void trigger() {
        for (var listener : listeners) {
            listener.run();
        }
    }

    public void register(Runnable run) {
        listeners.add(run);
    }

    public void unregister(Runnable run) {
        listeners.remove(run);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.trigger();
    }
}
