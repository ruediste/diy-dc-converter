package com.github.ruediste;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.google.common.io.MoreFiles;

public class InterfaceCCodeGenerator {

    public static void main(String... args) throws IOException {
        var destDir = Path.of("..", "fw", "src", "interface");
        Files.createDirectories(destDir);
        MoreFiles.deleteDirectoryContents(destDir);
        var interfaceClasses = InterfaceLoader.load();
        try (var header = new FileWriter(destDir.resolve("messages.h").toFile(), StandardCharsets.UTF_8);
                var impl = new FileWriter(destDir.resolve("messages.cpp").toFile(), StandardCharsets.UTF_8)) {

            header.append("#ifndef MESSAGES_H\n");
            header.append("#define MESSAGES_H\n\n");
            header.append("#include <stdint.h>\n\n");
            header.append("extern int messageSizes[];\n\n");

            for (var info : interfaceClasses) {
                header.append("struct __attribute__((packed)) " + info.simpleName + "{\n");
                for (var field : info.fields) {
                    header.append("  " + field.cType + " " + field.name + ";\n");
                }
                header.append("};\n\n");
            }

            header.append("enum class MessageType : uint8_t {\n");
            for (var info : interfaceClasses) {
                header.append("  " + info.simpleName + "=" + info.id + ",\n");
            }
            header.append("};\n\n");

            header.append("const int maxMessageSize=" + interfaceClasses.stream().map(cls -> cls.size())
                    .collect(Collectors.maxBy(Comparator.naturalOrder())).get() + ";\n");
            header.append("#endif\n");

            impl.append("#include \"messages.h\"\n\n");
            impl.append("int messageSizes[] = {\n");
            for (var info : interfaceClasses)
                impl.append("  sizeof(" + info.simpleName + "), // expected size: "
                        + info.size() + "\n");
            impl.append("};\n");
        }

    }
}
