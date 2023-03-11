package com.github.ruediste;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.github.ruediste.messages.TestMessage;

public class InterfaceSerializerTest {
    @Test
    public void roundTrip() {
        var serializer = new InterfaceSerializer();
        var msg = new TestMessage();
        msg.uint16Field = 0xabcd;
        msg.floatField = (float) Math.PI;
        var out = new ByteArrayOutputStream();
        serializer.serialize(msg, out);
        var msg2 = (TestMessage) serializer.deserialize(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(msg.uint16Field, msg2.uint16Field);
        assertEquals(msg.floatField, msg2.floatField);
    }
}
