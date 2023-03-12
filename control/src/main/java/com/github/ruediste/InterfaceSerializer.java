package com.github.ruediste;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.ruediste.InterfaceLoader.InterfaceClass;

public class InterfaceSerializer {
    private List<InterfaceClass> interfaceClasses = InterfaceLoader.load();
    private Map<Class<?>, InterfaceClass> interfaceClassesByCls = new HashMap<>();
    private Map<Integer, InterfaceClass> interfaceClassesById = new HashMap<>();

    InterfaceSerializer() {
        interfaceClasses = InterfaceLoader.load();
        initializeMaps();
    }

    private void initializeMaps() {
        for (var cls : interfaceClasses) {
            interfaceClassesByCls.put(cls.cls, cls);
            interfaceClassesById.put(cls.id, cls);
        }
    }

    public void serialize(Object msg, OutputStream out) {
        try {
            var cls = interfaceClassesByCls.get(msg.getClass());
            if (cls == null)
                throw new RuntimeException("Unknown interface class " + msg.getClass());

            out.write(cls.id);
            for (var field : cls.fields) {
                field.writer.write(field.field.get(msg), out);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object deserialize(InputStream in) {
        try {
            int id = in.read();
            var cls = interfaceClassesById.get(id);
            if (cls == null)
                throw new RuntimeException("Unknown interface id " + id);

            var msg = cls.cls.getConstructor().newInstance();
            for (var field : cls.fields) {
                field.field.set(msg, field.reader.read(in));
            }
            return msg;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
