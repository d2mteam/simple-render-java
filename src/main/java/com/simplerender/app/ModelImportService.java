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
        logger.info("Loading plugins from {}", pluginManager.getPluginsRoot());
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
        pluginManager.getPlugins().forEach(plugin -> logger.info(
            "Plugin loaded: {} ({})",
            plugin.getDescriptor().getPluginId(),
            plugin.getPluginPath()
        ));
        List<ModelImporter> importers = pluginManager.getExtensions(ModelImporter.class);
        if (importers.isEmpty()) {
            logger.warn("No model importers found. Ensure plugin jars or classes are built.");
        } else {
            for (ModelImporter importer : importers) {
                logger.info("Model importer available: {} supports {}",
                    importer.getClass().getSimpleName(),
                    String.join(", ", importer.supportedExtensions())
                );
            }
        }
    }

    public Optional<ModelImporter.ImportedModel> importModel(Path path) {
        String extension = extension(path);
        logger.info("Attempting to import model {} (extension: {})", path, extension);
        List<ModelImporter> importers = pluginManager.getExtensions(ModelImporter.class);
        for (ModelImporter importer : importers) {
            if (importerSupports(importer, extension)) {
                logger.info("Using importer {} for {}", importer.getClass().getSimpleName(), path);
                return Optional.of(importer.importModel(path));
            }
        }
        if (importers.isEmpty()) {
            logger.error("No importers registered. Plugins may not be loaded correctly.");
        } else {
            String supported = importers.stream()
                .flatMap(importer -> Stream.of(importer.supportedExtensions()))
                .distinct()
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
            logger.error("No importer found for extension {}. Supported: {}", extension, supported);
        }
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
        prepareDevelopmentPlugins(devPlugins);
        return new ModelImportService(devPlugins);
    }

    private static void prepareDevelopmentPlugins(Path devPlugins) {
        if (!Files.exists(devPlugins)) {
            return;
        }
        try (Stream<Path> pluginDirs = Files.list(devPlugins)) {
            pluginDirs.filter(Files::isDirectory).forEach(pluginDir -> {
                Path descriptor = pluginDir.resolve("plugin.properties");
                if (Files.exists(descriptor)) {
                    return;
                }
                Path resourceDescriptor = pluginDir.resolve(Paths.get("src", "main", "resources", "plugin.properties"));
                if (Files.exists(resourceDescriptor)) {
                    try {
                        Files.copy(resourceDescriptor, descriptor, StandardCopyOption.REPLACE_EXISTING);
                        logger.info("Copied plugin descriptor for {}", pluginDir.getFileName());
                    } catch (IOException e) {
                        logger.warn("Failed to copy plugin descriptor for {}", pluginDir.getFileName(), e);
                    }
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to prepare development plugins", e);
        }
    }
}
