package com.simplerender.app;

import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.gl.OpenGLRenderer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RenderControlPanel {
    private static final Logger logger = LoggerFactory.getLogger(RenderControlPanel.class);
    private static final AtomicBoolean started = new AtomicBoolean(false);

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
        stage.setTitle("Render Sidebar");
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
        root.setLeft(controls);

        HBox spacer = new HBox();
        spacer.setPrefWidth(8);
        root.setCenter(spacer);

        stage.setScene(new javafx.scene.Scene(root, 320, 520));
        stage.show();
        anchorSidebarToRender(stage, renderer);
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

    private static void anchorSidebarToRender(Stage stage, OpenGLRenderer renderer) {
        try {
            int[] x = new int[1];
            int[] y = new int[1];
            GLFW.glfwGetWindowPos(renderer.windowHandle(), x, y);
            double sidebarWidth = stage.getWidth() > 0 ? stage.getWidth() : 320;
            stage.setX(x[0] - sidebarWidth - 12);
            stage.setY(y[0]);
        } catch (Exception e) {
            logger.warn("Failed to anchor sidebar to render window", e);
        }
    }
}
