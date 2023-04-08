package com.github.ruediste;

public class AdcValuesMessage implements InterfaceMessage {
    @Datatype.array(2)
    @Datatype.uint16
    public int[] values;
}
