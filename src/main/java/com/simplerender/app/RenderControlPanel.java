package com.simplerender.app;

import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.gl.OpenGLRenderer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
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

        CheckBox toneMapping = new CheckBox("Tone Mapping");
        toneMapping.setSelected(true);
        toneMapping.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setToneMappingEnabled(newValue))
        );

        CheckBox bloom = new CheckBox("Bloom");
        bloom.setSelected(true);
        bloom.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setBloomEnabled(newValue))
        );

        CheckBox colorGrading = new CheckBox("Color Grading");
        colorGrading.setSelected(true);
        colorGrading.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setColorGradingEnabled(newValue))
        );

        CheckBox depthOfField = new CheckBox("Depth of Field");
        depthOfField.setSelected(false);
        depthOfField.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setDepthOfFieldEnabled(newValue))
        );

        CheckBox motionBlur = new CheckBox("Motion Blur");
        motionBlur.setSelected(false);
        motionBlur.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setMotionBlurEnabled(newValue))
        );

        CheckBox vignette = new CheckBox("Vignette");
        vignette.setSelected(true);
        vignette.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setVignetteEnabled(newValue))
        );

        CheckBox filmGrain = new CheckBox("Film Grain");
        filmGrain.setSelected(true);
        filmGrain.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setFilmGrainEnabled(newValue))
        );

        CheckBox ssao = new CheckBox("SSAO");
        ssao.setSelected(false);
        ssao.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setSsaoEnabled(newValue))
        );

        CheckBox ssr = new CheckBox("SSR");
        ssr.setSelected(false);
        ssr.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setSsrEnabled(newValue))
        );

        CheckBox ssgi = new CheckBox("SSGI");
        ssgi.setSelected(false);
        ssgi.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setSsgiEnabled(newValue))
        );

        CheckBox contactShadows = new CheckBox("Contact Shadows");
        contactShadows.setSelected(false);
        contactShadows.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setContactShadowsEnabled(newValue))
        );

        CheckBox rayTracing = new CheckBox("Ray Tracing");
        rayTracing.setSelected(true);
        rayTracing.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setRayTracingEnabled(newValue))
        );

        CheckBox rayTracingShadows = new CheckBox("Ray Tracing Shadows");
        rayTracingShadows.setSelected(true);
        rayTracingShadows.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setRayTracingShadowsEnabled(newValue))
        );

        CheckBox rayTracingReflections = new CheckBox("Ray Tracing Reflections");
        rayTracingReflections.setSelected(true);
        rayTracingReflections.selectedProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setRayTracingReflectionsEnabled(newValue))
        );

        Slider exposure = createSlider(0.1, 5.0, 1.0);
        exposure.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setExposure(newValue.floatValue()))
        );

        Slider bloomStrength = createSlider(0.0, 2.0, 0.35);
        bloomStrength.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setBloomStrength(newValue.floatValue()))
        );

        Slider bloomThreshold = createSlider(0.0, 2.0, 1.0);
        bloomThreshold.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setBloomThreshold(newValue.floatValue()))
        );

        Slider colorGradeSaturation = createSlider(0.0, 2.0, 1.0);
        colorGradeSaturation.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setColorGradeSaturation(newValue.floatValue()))
        );

        Slider vignetteIntensity = createSlider(0.0, 1.0, 0.35);
        vignetteIntensity.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setVignetteIntensity(newValue.floatValue()))
        );

        Slider filmGrainIntensity = createSlider(0.0, 0.2, 0.06);
        filmGrainIntensity.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setFilmGrainIntensity(newValue.floatValue()))
        );

        Slider dofFocus = createSlider(0.0, 1.0, 0.4);
        dofFocus.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setDofFocus(newValue.floatValue()))
        );

        Slider dofScale = createSlider(0.0, 10.0, 3.0);
        dofScale.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setDofScale(newValue.floatValue()))
        );

        Slider motionBlurStrength = createSlider(0.0, 1.0, 0.35);
        motionBlurStrength.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setMotionBlurStrength(newValue.floatValue()))
        );

        Slider ssaoStrength = createSlider(0.0, 2.0, 0.6);
        ssaoStrength.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setSsaoStrength(newValue.floatValue()))
        );

        Slider ssaoRadius = createSlider(0.0, 0.1, 0.02);
        ssaoRadius.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setSsaoRadius(newValue.floatValue()))
        );

        Slider ssrStrength = createSlider(0.0, 1.0, 0.35);
        ssrStrength.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setSsrStrength(newValue.floatValue()))
        );

        Slider ssgiStrength = createSlider(0.0, 1.0, 0.35);
        ssgiStrength.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setSsgiStrength(newValue.floatValue()))
        );

        Slider contactShadowStrength = createSlider(0.0, 1.0, 0.5);
        contactShadowStrength.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setContactShadowStrength(newValue.floatValue()))
        );

        Slider rayTracingMix = createSlider(0.0, 1.0, 0.85);
        rayTracingMix.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setRayTracingMix(newValue.floatValue()))
        );

        Slider rayTracingBounces = createSlider(1.0, 8.0, 2.0);
        rayTracingBounces.setMajorTickUnit(1.0);
        rayTracingBounces.setMinorTickCount(0);
        rayTracingBounces.setSnapToTicks(true);
        rayTracingBounces.valueProperty().addListener((obs, oldValue, newValue) ->
            renderer.updateScreenSpaceSettings(settings -> settings.setRayTracingMaxBounces((int) Math.round(newValue.doubleValue())))
        );

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

        GridPane postGrid = new GridPane();
        postGrid.setHgap(8);
        postGrid.setVgap(8);
        int postRow = 0;
        addSliderRow(postGrid, postRow++, "Exposure", exposure);
        addSliderRow(postGrid, postRow++, "Bloom Strength", bloomStrength);
        addSliderRow(postGrid, postRow++, "Bloom Threshold", bloomThreshold);
        addSliderRow(postGrid, postRow++, "Color Saturation", colorGradeSaturation);
        addSliderRow(postGrid, postRow++, "Vignette Intensity", vignetteIntensity);
        addSliderRow(postGrid, postRow++, "Film Grain Intensity", filmGrainIntensity);
        addSliderRow(postGrid, postRow++, "DOF Focus", dofFocus);
        addSliderRow(postGrid, postRow++, "DOF Scale", dofScale);
        addSliderRow(postGrid, postRow++, "Motion Blur Strength", motionBlurStrength);
        addSliderRow(postGrid, postRow++, "SSAO Strength", ssaoStrength);
        addSliderRow(postGrid, postRow++, "SSAO Radius", ssaoRadius);
        addSliderRow(postGrid, postRow++, "SSR Strength", ssrStrength);
        addSliderRow(postGrid, postRow++, "SSGI Strength", ssgiStrength);
        addSliderRow(postGrid, postRow++, "Contact Shadow Strength", contactShadowStrength);
        addSliderRow(postGrid, postRow++, "Ray Tracing Mix", rayTracingMix);
        addSliderRow(postGrid, postRow, "Ray Bounces", rayTracingBounces);

        VBox postToggles = new VBox(6);
        postToggles.getChildren().addAll(
            toneMapping,
            bloom,
            colorGrading,
            depthOfField,
            motionBlur,
            vignette,
            filmGrain,
            ssao,
            ssr,
            ssgi,
            contactShadows,
            rayTracing,
            rayTracingShadows,
            rayTracingReflections
        );

        VBox postControls = new VBox(8, postToggles, postGrid);

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
            applyShader,
            new Label("Post Processing"),
            postControls
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

    private static Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit((max - min) / 4.0);
        slider.setBlockIncrement((max - min) / 100.0);
        slider.setPrefWidth(190);
        return slider;
    }

    private static void addSliderRow(GridPane grid, int row, String label, Slider slider) {
        grid.add(new Label(label), 0, row);
        grid.add(slider, 1, row);
    }
}
