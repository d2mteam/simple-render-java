package com.simplerender.app;

import com.simplerender.gl.OpenGLRenderer;
import com.simplerender.scene.Scene;
import com.simplerender.asset.plugin.ModelImporter;

import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameApplication {
    private static final Logger logger = LoggerFactory.getLogger(GameApplication.class);

    private final EngineConfig config;
    private final GameLoop gameLoop;
    private final Scene scene;
    private final OpenGLRenderer renderer;
    private final JavaFxInputAdapter inputAdapter;

    public GameApplication(EngineConfig config) {
        this.config = config;
        this.renderer = new OpenGLRenderer();
        this.inputAdapter = new JavaFxInputAdapter();
        if (config.shaderName() != null && !config.shaderName().isBlank()) {
            this.renderer.requestShader(config.shaderName());
        }
        this.renderer.init();
        ModelImportService importService = ModelImportService.defaultService();
        importService.loadPlugins();
        String configModelPath = config.modelPath();
        Optional<Path> modelPath;
        Optional<ModelImporter.ImportedModel> importedModel;
        if (configModelPath != null && !configModelPath.isBlank()) {
            Path path = Path.of(configModelPath);
            modelPath = Optional.of(path);
            logger.info("Model import source: config path ({})", path);
            importedModel = importService.importModel(path);
            if (importedModel.isEmpty()) {
                throw new IllegalStateException("Configured model could not be imported: " + path);
            }
        } else {
            logger.info("Model import source: file dialog");
            modelPath = ModelFileDialog.chooseModelFile();
            modelPath.ifPresent(path -> logger.info("Selected model path: {}", path));
            importedModel = modelPath.flatMap(importService::importModel);
            if (modelPath.isPresent() && importedModel.isEmpty()) {
                throw new IllegalStateException("Selected model could not be imported.");
            }
        }
        if (importedModel.isPresent()) {
            logger.info("Imported model ready for rendering");
        } else {
            logger.info("No model selected; scene will start empty");
        }
        this.scene = Scene.bootstrap(renderer, importedModel);
        RenderControlPanel.launch(scene, renderer, importService, inputAdapter);
        this.gameLoop = new GameLoop(config, new Time(), scene, renderer, inputAdapter);
    }

    public void run() {
        logger.info("GameApplication run started");
        gameLoop.run();
        logger.info("GameApplication run completed");
    }
}
