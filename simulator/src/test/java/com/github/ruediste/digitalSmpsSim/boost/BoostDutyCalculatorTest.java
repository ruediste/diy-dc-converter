package com.github.ruediste.digitalSmpsSim.boost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BoostDutyCalculatorTest {
    @Test
    public void test() {
        var design = new BoostDutyCalculator();

        // DCM values
        design.switchingFrequency = 10.343e3;
        design.inputVoltage = 5;
        design.outputVoltage = 12;
        design.outputCurrent = 0.010;
        design.inductance = 3.76e-3;
        var result = design.calculate();
        assertEquals(46.67, 100 * result.duty, 0.1);
        assertEquals(0, result.initialInductorCurrent, 1e-3);

        // raise switching frequency to enter CCM region
        design.switchingFrequency *= 10;
        result = design.calculate();
        assertEquals(58.33, 100 * result.duty, 0.1);
        assertEquals(0.020, result.initialInductorCurrent, 1e-3);
    }
}
