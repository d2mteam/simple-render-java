package com.simplerender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the layering of the engine. Each layer may only depend on the layers below it:
 * <pre>
 *   engine  ─►  render (+ render.gl)  ─►  scene  ─►  asset  ─►  math
 * </pre>
 * and only the render layer talks to OpenGL (LWJGL). The UI lives in another Gradle module,
 * so the compiler already stops the engine from using JavaFX.
 */
class ArchitectureTest {
    private static final Path SOURCES = Path.of("src/main/java");

    /** Package prefix -> import prefixes it must not use. */
    private static final Map<String, List<String>> FORBIDDEN_IMPORTS = Map.of(
            "com.simplerender.math", List.of("com.simplerender.asset", "com.simplerender.scene",
                    "com.simplerender.render", "com.simplerender.engine", "org.lwjgl"),
            "com.simplerender.asset", List.of("com.simplerender.scene", "com.simplerender.render",
                    "com.simplerender.engine", "org.lwjgl"),
            "com.simplerender.scene", List.of("com.simplerender.render", "com.simplerender.engine", "org.lwjgl"),
            "com.simplerender.render", List.of("com.simplerender.engine"));

    @Test
    void layersOnlyDependDownwards() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String pkg = SOURCES.relativize(file.getParent()).toString().replace('/', '.').replace('\\', '.');
                for (String line : Files.readAllLines(file)) {
                    if (!line.startsWith("import ")) {
                        continue;
                    }
                    String imported = line.substring("import ".length()).replace("static ", "").replace(";", "").trim();
                    FORBIDDEN_IMPORTS.forEach((layer, forbidden) -> {
                        if (pkg.equals(layer) || pkg.startsWith(layer + ".")) {
                            forbidden.stream()
                                    .filter(imported::startsWith)
                                    .forEach(f -> violations.add(file.getFileName() + " imports " + imported));
                        }
                    });
                    if (imported.startsWith("javafx") || imported.startsWith("java.awt")
                            || imported.startsWith("javax.swing")) {
                        violations.add(file.getFileName() + " imports UI toolkit " + imported);
                    }
                }
            }
        }
        assertEquals(List.of(), violations);
    }
}
