package com.github.ruediste.digitalSmpsSim.boost;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.digitalSmpsSim.simulation.Circuit;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;

public class HardwareTimer extends CircuitElement {
    protected HardwareTimer(Circuit circuit) {
        super(circuit);
    }

    protected HardwareTimer(CircuitElement element) {
        this(element, null);
    }

    protected HardwareTimer(CircuitElement element, Runnable onOverflow) {
        super(element.circuit);
        this.onOverflow = onOverflow;
    }

    public double clockFrequency;

    public long overflow;

    public Runnable onOverflow;

    public static class Channel {
        public long compare;

        public Runnable onCompare;

        private double nextCompareMatchInstant;
        private boolean matched;
    }

    private List<Channel> channels = new ArrayList<>();
    private double lastCycleStart = 0;
    private double nextCycleStart;

    private void updateInstants() {
        var clockPeriod = 1 / clockFrequency;
        nextCycleStart = lastCycleStart + clockPeriod * overflow;
        for (var channel : channels) {
            channel.nextCompareMatchInstant = lastCycleStart + clockPeriod * channel.compare;
            channel.matched = false;
        }
    }

    @Override
    public void initialize() {
        updateInstants();
    }

    @Override
    public Double stepEndTime(double stepStart) {
        Double result = null;
        if (stepStart < nextCycleStart)
            result = nextCycleStart;

        for (var channel : channels) {
            if (stepStart < channel.nextCompareMatchInstant) {
                result = result == null ? channel.nextCompareMatchInstant
                        : Math.min(result, channel.nextCompareMatchInstant);
            }
        }
        return result;
    }

    @Override
    public void run(double stepStart, double stepEnd, double stepDuration) {
        for (var channel : channels) {
            if (!channel.matched && stepEnd >= channel.nextCompareMatchInstant) {
                channel.onCompare.run();
                channel.matched = true;
            }
        }
        if (stepEnd <= nextCycleStart) {
            if (onOverflow != null)
                onOverflow.run();
            lastCycleStart = nextCycleStart;
            updateInstants();
        }
    }

    public Channel createChannel(Runnable onCompare) {
        var result = new Channel();
        result.onCompare = onCompare;
        channels.add(result);
        return result;
    }
}
