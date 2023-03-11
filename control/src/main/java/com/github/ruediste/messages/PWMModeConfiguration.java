package com.github.ruediste.messages;

import com.github.ruediste.Datatype;

public class PWMModeConfiguration {
    @Datatype.uint16
    public int limit;

    @Datatype.uint16
    public int compare;

    public float test;
}
