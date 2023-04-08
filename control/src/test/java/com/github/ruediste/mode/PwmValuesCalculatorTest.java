package com.github.ruediste.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PwmValuesCalculatorTest {

    @Test
    public void test() {
        var calc = new PwmValuesCalculator();
        var values = calc.calculate(10e3, 0.5);
        assertEquals(0, values.prescale);
        assertEquals(8400, values.reload);
        assertEquals(4200, values.compare);

        values = calc.calculate(100e3, 0.5);
        assertEquals(0, values.prescale);
        assertEquals(840, values.reload);
        assertEquals(420, values.compare);

        values = calc.calculate(1, 0.5);
        assertEquals(1281, values.prescale);
        assertEquals(65523, values.reload);
        assertEquals(32762, values.compare);

        values = calc.calculate(2, 0.1);
        assertEquals(640, values.prescale);
        assertEquals(65523, values.reload);
        assertEquals(6552, values.compare);
    }
}
