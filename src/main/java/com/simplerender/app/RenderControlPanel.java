package com.simplerender.app;

import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.gl.OpenGLRenderer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RenderControlPanel {
    private static final Logger logger = LoggerFactory.getLogger(RenderControlPanel.class);
    private static final AtomicBoolean started = new AtomicBoolean(false);
    private static final int CONTROL_WIDTH = 320;
    private static final int DEFAULT_RENDER_WIDTH = 960;
    private static final int DEFAULT_RENDER_HEIGHT = 600;
    private static final int GAP = 12;

    private RenderControlPanel() {
    }

    public static void launch(
        com.simplerender.scene.Scene scene,
        OpenGLRenderer renderer,
        ModelImportService importService,
        JavaFxInputAdapter inputAdapter
    ) {
        if (started.compareAndSet(false, true)) {
            Platform.startup(() -> showStage(scene, renderer, importService, inputAdapter));
        } else {
            Platform.runLater(() -> showStage(scene, renderer, importService, inputAdapter));
        }
    }

    private static void showStage(
        com.simplerender.scene.Scene scene,
        OpenGLRenderer renderer,
        ModelImportService importService,
        JavaFxInputAdapter inputAdapter
    ) {
        Stage stage = new Stage();
        stage.setTitle("Simple Render");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        TextField posX = new TextField("0");
        TextField posY = new TextField("0");
        TextField posZ = new TextField("0");
        TextField scale = new TextField("1");

        ListView<String> objectList = new ListView<>();
        refreshObjectList(scene, objectList);

        ComboBox<String> shaderSelect = new ComboBox<>();
        shaderSelect.getItems().addAll("default", "disney_brdf");
        shaderSelect.getSelectionModel().selectFirst();

        Button loadObject = new Button("Load OBJ/GLTF");
        loadObject.setOnAction(event -> {
            Optional<Path> modelPath = ModelFileDialog.chooseModelFile();
            if (modelPath.isEmpty()) {
                return;
            }
            Optional<ModelImporter.ImportedModel> imported = modelPath.flatMap(importService::importModel);
            if (imported.isEmpty()) {
                logger.warn("Failed to import model from {}", modelPath.get());
                return;
            }
            renderer.submit(() -> scene.addImportedObject(renderer, imported.get()))
                .whenComplete((index, error) -> {
                    if (error != null) {
                        logger.error("Failed to upload imported model {}", modelPath.get(), error);
                        return;
                    }
                    Platform.runLater(() -> {
                        refreshObjectList(scene, objectList);
                        objectList.getSelectionModel().select(index);
                    });
                });
        });

        Button applyTransform = new Button("Apply Transform");
        applyTransform.setOnAction(event -> {
            try {
                float x = Float.parseFloat(posX.getText());
                float y = Float.parseFloat(posY.getText());
                float z = Float.parseFloat(posZ.getText());
                float s = Float.parseFloat(scale.getText());
                int index = objectList.getSelectionModel().getSelectedIndex();
                if (index < 0) {
                    logger.warn("No object selected for transform");
                    return;
                }
                scene.updateTransform(index, x, y, z, s);
            } catch (NumberFormatException e) {
                logger.warn("Invalid transform input", e);
            }
        });

        Button applyShader = new Button("Apply Shader");
        applyShader.setOnAction(event -> {
            String selected = shaderSelect.getSelectionModel().getSelectedItem();
            renderer.requestShader(selected);
            logger.info("Requested shader {}", selected);
        });

        GridPane transformGrid = new GridPane();
        transformGrid.setHgap(8);
        transformGrid.setVgap(8);
        transformGrid.add(new Label("Position X"), 0, 0);
        transformGrid.add(posX, 1, 0);
        transformGrid.add(new Label("Position Y"), 0, 1);
        transformGrid.add(posY, 1, 1);
        transformGrid.add(new Label("Position Z"), 0, 2);
        transformGrid.add(posZ, 1, 2);
        transformGrid.add(new Label("Scale"), 0, 3);
        transformGrid.add(scale, 1, 3);

        VBox controls = new VBox(10);
        controls.getChildren().addAll(
            new Label("Objects"),
            objectList,
            loadObject,
            new Label("Transform"),
            transformGrid,
            applyTransform,
            new Label("Shader"),
            shaderSelect,
            applyShader
        );
        controls.setAlignment(Pos.TOP_LEFT);
        controls.setPrefWidth(CONTROL_WIDTH);
        root.setLeft(controls);

        ImageView renderView = new ImageView(new WritableImage(DEFAULT_RENDER_WIDTH, DEFAULT_RENDER_HEIGHT));
        renderView.setPreserveRatio(false);
        renderView.setSmooth(false);

        StackPane renderPane = new StackPane(renderView);
        renderPane.setMinSize(400, 300);
        renderPane.setPrefSize(DEFAULT_RENDER_WIDTH, DEFAULT_RENDER_HEIGHT);
        renderView.fitWidthProperty().bind(renderPane.widthProperty());
        renderView.fitHeightProperty().bind(renderPane.heightProperty());
        root.setCenter(renderPane);

        RenderFrameBridge frameBridge = new RenderFrameBridge(renderView);
        renderer.setFrameBridge(frameBridge);

        stage.setMinWidth(CONTROL_WIDTH + GAP + 400);
        stage.setMinHeight(360);
        Scene uiScene = new javafx.scene.Scene(root, CONTROL_WIDTH + GAP + DEFAULT_RENDER_WIDTH, DEFAULT_RENDER_HEIGHT + 24);
        stage.setScene(uiScene);
        stage.show();
        inputAdapter.attach(uiScene, renderPane);
        renderPane.widthProperty().addListener((obs, oldValue, newValue) -> requestResize(renderer, renderPane));
        renderPane.heightProperty().addListener((obs, oldValue, newValue) -> requestResize(renderer, renderPane));
        Platform.runLater(() -> requestResize(renderer, renderPane));
        stage.setOnCloseRequest(event -> renderer.requestStop());
        logger.info("Render control panel opened");
    }

    private static void refreshObjectList(com.simplerender.scene.Scene scene, ListView<String> objectList) {
        objectList.getItems().clear();
        for (int i = 0; i < scene.objectCount(); i++) {
            objectList.getItems().add("Object " + i);
        }
        if (!objectList.getItems().isEmpty() && objectList.getSelectionModel().getSelectedIndex() < 0) {
            objectList.getSelectionModel().selectFirst();
        }
    }

    private static void requestResize(OpenGLRenderer renderer, StackPane renderPane) {
        double width = renderPane.getWidth();
        double height = renderPane.getHeight();
        if (width <= 1 || height <= 1) {
            return;
        }
        renderer.requestResize((int) Math.round(width), (int) Math.round(height));
    }
}
