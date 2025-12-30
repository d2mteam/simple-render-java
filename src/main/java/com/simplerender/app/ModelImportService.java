package com.simplerender.app;

import com.simplerender.asset.plugin.ModelImporter;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Stream;

public final class ModelImportService {
    private static final Logger logger = LoggerFactory.getLogger(ModelImportService.class);

    private final PluginManager pluginManager;

    public ModelImportService(Path pluginsDir) {
        this.pluginManager = new SimpleRenderPluginManager(pluginsDir);
    }

    public void loadPlugins() {
        try {
            pluginManager.loadPlugins();
        } catch (Exception e) {
            logger.warn("Failed to load plugins from directory", e);
        }
        try {
            pluginManager.startPlugins();
        } catch (Exception e) {
            logger.warn("Failed to start plugins", e);
        }
        logger.info("Loaded {} plugins", pluginManager.getPlugins().size());
    }

    public Optional<ModelImporter.ImportedModel> importModel(Path path) {
        String extension = extension(path);
        List<ModelImporter> importers = pluginManager.getExtensions(ModelImporter.class);
        for (ModelImporter importer : importers) {
            if (importerSupports(importer, extension)) {
                return Optional.of(importer.importModel(path));
            }
        }
        logger.error("No importer found for extension {}", extension);
        return Optional.empty();
    }

    private boolean importerSupports(ModelImporter importer, String extension) {
        for (String supported : importer.supportedExtensions()) {
            if (supported.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }

    public static ModelImportService defaultService() {
        Path buildPlugins = Paths.get("build", "plugins");
        if (buildPlugins.toFile().exists()) {
            return new ModelImportService(buildPlugins);
        }
        Path devPlugins = Paths.get("plugins");
        Path devBuildLibs = Paths.get("plugins").toAbsolutePath();
        try (Stream<Path> libs = Files.exists(devBuildLibs) ? Files.walk(devBuildLibs, 3) : Stream.empty()) {
            List<Path> jars = new ArrayList<>();
            libs.filter(path -> path.toString().endsWith(".jar"))
                .filter(path -> path.toString().contains("build/libs"))
                .forEach(jars::add);
            if (!jars.isEmpty()) {
                Files.createDirectories(buildPlugins);
                for (Path jar : jars) {
                    Path target = buildPlugins.resolve(jar.getFileName().toString());
                    Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return new ModelImportService(buildPlugins);
            }
        } catch (IOException e) {
            logger.warn("Failed to prepare plugin jars", e);
        }
        return new ModelImportService(devPlugins);
    }
}
