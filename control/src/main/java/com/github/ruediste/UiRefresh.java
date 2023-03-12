package com.github.ruediste;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.inject.Singleton;

@Singleton
public class UiRefresh implements ActionListener {
    private Set<Runnable> listeners = ConcurrentHashMap.newKeySet();

    private volatile Object triggerToken;

    private ThreadLocal<Integer> suspended = new ThreadLocal<>();

    /** Thread safe */
    public void suspend() {
        Integer tmp = suspended.get();
        suspended.set(tmp == null ? 1 : tmp + 1);
    }

    /** Thread safe */
    public void resume() {
        Integer tmp = suspended.get();
        if (tmp == null) {
            throw new RuntimeException("Unbalanced call to resume()");
        }
        if (tmp == 1) {
            suspended.remove();
            trigger();
        } else
            suspended.set(tmp - 1);
    }

    /** Thread safe */
    public void trigger() {
        if (suspended.get() != null)
            return;
        var token = new Object();
        this.triggerToken = token;
        javax.swing.SwingUtilities.invokeLater(() -> {
            for (var listener : listeners) {
                if (this.triggerToken != token)
                    return;
                listener.run();
            }
        });
    }

    /** Thread safe */
    public void register(Runnable run) {
        listeners.add(run);
    }

    /** Thread safe */
    public void unregister(Runnable run) {
        listeners.remove(run);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.trigger();
    }
}
