package com.github.ruediste;

public class SystemStatusMessage implements InterfaceMessage {
    @Datatype.array(2)
    @Datatype.uint16
    public int[] adcValues;

    public float controlCpuUsageFraction;
}
