package com.simplerender.app;

import com.simplerender.asset.plugin.ModelImporter;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

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
        return new ModelImportService(Paths.get("plugins"));
    }
}
