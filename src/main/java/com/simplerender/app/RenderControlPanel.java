package com.simplerender.app;

import com.simplerender.gl.OpenGLRenderer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RenderControlPanel {
    private static final Logger logger = LoggerFactory.getLogger(RenderControlPanel.class);
    private static final AtomicBoolean started = new AtomicBoolean(false);

    private RenderControlPanel() {
    }

    public static void launch(com.simplerender.scene.Scene scene, OpenGLRenderer renderer) {
        if (started.compareAndSet(false, true)) {
            Platform.startup(() -> showStage(scene, renderer));
        } else {
            Platform.runLater(() -> showStage(scene, renderer));
        }
    }

    private static void showStage(com.simplerender.scene.Scene scene, OpenGLRenderer renderer) {
        Stage stage = new Stage();
        stage.setTitle("Render Controls");
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(8);
        grid.setVgap(8);

        TextField posX = new TextField("0");
        TextField posY = new TextField("0");
        TextField posZ = new TextField("0");
        TextField scale = new TextField("1");

        ComboBox<String> shaderSelect = new ComboBox<>();
        shaderSelect.getItems().addAll("default", "disney_brdf");
        shaderSelect.getSelectionModel().selectFirst();

        Button applyTransform = new Button("Apply Transform");
        applyTransform.setOnAction(event -> {
            try {
                float x = Float.parseFloat(posX.getText());
                float y = Float.parseFloat(posY.getText());
                float z = Float.parseFloat(posZ.getText());
                float s = Float.parseFloat(scale.getText());
                scene.updatePrimaryTransform(x, y, z, s);
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

        grid.add(new Label("Position X"), 0, 0);
        grid.add(posX, 1, 0);
        grid.add(new Label("Position Y"), 0, 1);
        grid.add(posY, 1, 1);
        grid.add(new Label("Position Z"), 0, 2);
        grid.add(posZ, 1, 2);
        grid.add(new Label("Scale"), 0, 3);
        grid.add(scale, 1, 3);
        grid.add(applyTransform, 0, 4, 2, 1);
        grid.add(new Label("Shader"), 0, 5);
        grid.add(shaderSelect, 1, 5);
        grid.add(applyShader, 0, 6, 2, 1);

        stage.setScene(new Scene(grid, 280, 260));
        stage.show();
        logger.info("Render control panel opened");
    }
}
