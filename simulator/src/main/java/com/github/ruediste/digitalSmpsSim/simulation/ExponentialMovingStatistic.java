package com.github.ruediste.digitalSmpsSim.simulation;

/**
 * Exponential Moving Average and Variance
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Exponential_smoothing"> Wikipedia
 *      </a>
 */
public class ExponentialMovingStatistic {
    private double alpha;

    public double average;
    public double variance;

    public ExponentialMovingStatistic(double averageAge, double samplePeriod) {
        alpha = 1 / (averageAge / samplePeriod + 1);
    }

    public void add(double value) {
        average = alpha * value + (1 - alpha) * average;
        double error = value - average;
        variance = alpha * error * error + (1 - alpha) * variance;
    }
}
