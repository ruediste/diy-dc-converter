package com.github.ruediste;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.common.io.MoreFiles;

public class InterfaceCCodeGenerator {

    public static void main(String... args) throws IOException {
        var destDir = Path.of("generated");
        Files.createDirectories(destDir);
        MoreFiles.deleteDirectoryContents(destDir);
        var interfaceClasses = InterfaceLoader.load();
        try (
                var header = new FileWriter(destDir.resolve("messages.h").toFile(), StandardCharsets.UTF_8);
                var impl = new FileWriter(destDir.resolve("messages.cpp").toFile(), StandardCharsets.UTF_8)) {
            for (var info : interfaceClasses) {
                header.append("struct " + info.simpleName + "\n");
                for (var field : info.fields) {
                    header.append("  " + field.cType + " " + field.name + ";\n");
                }
                header.append("};\n\n");
            }

            header.append("enum class MessageTypes {\n");
            {
                for (var info : interfaceClasses) {
                    header.append("  " + info.simpleName + "=" + info.id + ",\n");
                }
            }
            header.append("};\n");
        }

    }
}
