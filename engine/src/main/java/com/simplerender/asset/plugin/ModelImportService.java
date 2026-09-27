package com.simplerender.asset.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginManager;
import org.pf4j.PropertiesPluginDescriptorFinder;
import org.pf4j.RuntimeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the {@link ModelImporter} plugins and picks the right one for a file.
 *
 * <p>Plugins are read with PF4J in <em>development</em> mode: every folder under
 * {@code plugins/} with a {@code plugin.properties} file is a plugin, and its classes are
 * loaded from its Gradle {@code build/classes} output. This is what {@code gradle run} uses.
 */
public final class ModelImportService {
    private static final Logger logger = LoggerFactory.getLogger(ModelImportService.class);

    private final PluginManager pluginManager;
    private List<ModelImporter> importers = List.of();

    public ModelImportService(Path pluginsDir) {
        this.pluginManager = new DevelopmentPluginManager(pluginsDir);
    }

    /** Plugins live in {@code plugins/}, relative to the working directory. */
    public static ModelImportService createDefault() {
        return new ModelImportService(Path.of("plugins"));
    }

    /** Loads and starts every plugin, then collects the importers they provide. */
    public void loadPlugins() {
        logger.info("Loading plugins from {}", pluginManager.getPluginsRoots());
        try {
            pluginManager.loadPlugins();
            pluginManager.startPlugins();
        } catch (RuntimeException e) {
            logger.warn("Some plugins failed to load", e);
        }
        // An extension can be listed twice (generated index + checked-in index), so de-duplicate by class.
        Map<String, ModelImporter> unique = new LinkedHashMap<>();
        for (ModelImporter importer : pluginManager.getExtensions(ModelImporter.class)) {
            unique.putIfAbsent(importer.getClass().getName(), importer);
        }
        importers = List.copyOf(unique.values());
        if (importers.isEmpty()) {
            logger.warn("No model importers found. Did the plugins compile? (gradle build)");
        }
        for (ModelImporter importer : importers) {
            logger.info("Importer {} handles {}",
                    importer.getClass().getSimpleName(), String.join(", ", importer.supportedExtensions()));
        }
    }

    /** File extensions (without the dot) that some importer can read, e.g. {@code obj}. */
    public List<String> supportedExtensions() {
        List<String> extensions = new ArrayList<>();
        for (ModelImporter importer : importers) {
            for (String extension : importer.supportedExtensions()) {
                extensions.add(extension.toLowerCase(Locale.ROOT));
            }
        }
        return extensions;
    }

    /**
     * Imports a model file with the importer that handles its extension.
     *
     * @throws IllegalArgumentException if no importer handles the file type
     * @throws RuntimeException if the importer fails to read the file
     */
    public ModelImporter.ImportedModel importModel(Path path) {
        String extension = extensionOf(path);
        for (ModelImporter importer : importers) {
            for (String supported : importer.supportedExtensions()) {
                if (supported.equalsIgnoreCase(extension)) {
                    logger.info("Importing {} with {}", path, importer.getClass().getSimpleName());
                    return importer.importModel(path);
                }
            }
        }
        throw new IllegalArgumentException(
                "No importer for '." + extension + "' files (supported: " + supportedExtensions() + ")");
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** PF4J manager that always runs in development mode and reads {@code plugin.properties}. */
    private static final class DevelopmentPluginManager extends DefaultPluginManager {
        DevelopmentPluginManager(Path pluginsRoot) {
            super(pluginsRoot);
        }

        @Override
        public RuntimeMode getRuntimeMode() {
            return RuntimeMode.DEVELOPMENT;
        }

        @Override
        protected PluginDescriptorFinder createPluginDescriptorFinder() {
            return new PropertiesPluginDescriptorFinder();
        }
    }
}
