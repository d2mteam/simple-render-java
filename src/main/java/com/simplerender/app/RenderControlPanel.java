package com.simplerender.app;

import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.gl.OpenGLRenderer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
    private static final int RENDER_WIDTH = 960;
    private static final int RENDER_HEIGHT = 600;
    private static final int GAP = 8;

    private RenderControlPanel() {
    }

    public static void launch(com.simplerender.scene.Scene scene, OpenGLRenderer renderer, ModelImportService importService) {
        if (started.compareAndSet(false, true)) {
            Platform.startup(() -> showStage(scene, renderer, importService));
        } else {
            Platform.runLater(() -> showStage(scene, renderer, importService));
        }
    }

    private static void showStage(com.simplerender.scene.Scene scene, OpenGLRenderer renderer, ModelImportService importService) {
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

        Region renderPlaceholder = new Region();
        renderPlaceholder.setPrefSize(RENDER_WIDTH, RENDER_HEIGHT);
        root.setCenter(renderPlaceholder);

        stage.setScene(new javafx.scene.Scene(root, CONTROL_WIDTH + GAP + RENDER_WIDTH, RENDER_HEIGHT + 24));
        stage.show();
        stage.xProperty().addListener((obs, oldValue, newValue) -> syncRenderWindow(stage, root, renderer));
        stage.yProperty().addListener((obs, oldValue, newValue) -> syncRenderWindow(stage, root, renderer));
        stage.widthProperty().addListener((obs, oldValue, newValue) -> syncRenderWindow(stage, root, renderer));
        stage.heightProperty().addListener((obs, oldValue, newValue) -> syncRenderWindow(stage, root, renderer));
        Platform.runLater(() -> syncRenderWindow(stage, root, renderer));
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

    private static void syncRenderWindow(Stage stage, BorderPane root, OpenGLRenderer renderer) {
        try {
            Point2D origin = root.localToScreen(0, 0);
            if (origin == null) {
                return;
            }
            int x = (int) Math.round(origin.getX() + CONTROL_WIDTH + GAP);
            int y = (int) Math.round(origin.getY());
            renderer.setWindowSize(RENDER_WIDTH, RENDER_HEIGHT);
            renderer.setWindowPosition(x, y);
        } catch (Exception e) {
            logger.warn("Failed to sync render window position", e);
        }
    }
}
