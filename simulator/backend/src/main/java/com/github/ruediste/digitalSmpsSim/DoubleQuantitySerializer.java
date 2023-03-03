package com.github.ruediste.digitalSmpsSim;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.ruediste.digitalSmpsSim.quantity.DoubleQuantity;

public class DoubleQuantitySerializer extends StdSerializer<DoubleQuantity<?>> {

    protected DoubleQuantitySerializer(JavaType type) {
        super(type);
    }

    @Override
    public void serialize(DoubleQuantity<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeNumber(value.value());
    }

}
