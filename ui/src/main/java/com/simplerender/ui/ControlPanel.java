package com.simplerender.ui;

import com.simplerender.engine.Engine;
import com.simplerender.render.PostProcessSettings;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The side panel: load models, move them, drive the camera, pick a shader and tune the
 * post-processing effects.
 *
 * <p>Everything here runs on the JavaFX thread and talks to the engine only through
 * {@link Engine}'s thread-safe methods. Model files are imported on a background thread.
 */
final class ControlPanel {
    private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

    private final Engine engine;
    private final PostProcessSettings postProcess = new PostProcessSettings();
    private final ListView<String> objectList = new ListView<>();
    private final Label status = new Label();
    private final ExecutorService importThread = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "model-import");
        thread.setDaemon(true);
        return thread;
    });
    private final VBox content;

    ControlPanel(Engine engine, String initialShader) {
        this.engine = engine;
        content = new VBox(18,
                modelSection(),
                transformSection(),
                cameraSection(),
                shaderSection(initialShader),
                postProcessSection());
        content.setPadding(new Insets(12));
    }

    Node node() {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(310);
        return scroll;
    }

    void showStatus(String message) {
        status.setText(message);
    }

    /** Imports {@code path} in the background, then refreshes the object list. */
    void loadModel(Path path) {
        String fileName = path.getFileName().toString();
        showStatus("Loading " + fileName + "…");
        CompletableFuture.supplyAsync(() -> engine.loadModel(path), importThread)
                .whenComplete((added, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                        logger.error("Could not load {}", path, cause);
                        showStatus("Could not load " + fileName + ": " + cause.getMessage());
                        return;
                    }
                    objectList.getItems().setAll(engine.scene().objectNames());
                    showStatus("Loaded " + fileName + " (" + added.size() + " object(s))");
                }));
    }

    void shutdown() {
        importThread.shutdownNow();
    }

    // ---- Sections ---------------------------------------------------------------------------

    private Node modelSection() {
        Button load = new Button("Load model…");
        load.setOnAction(event -> chooseModelFile().ifPresent(this::loadModel));
        objectList.setPrefHeight(110);
        status.setWrapText(true);
        return section("Model", load, objectList, status);
    }

    private Node transformSection() {
        TextField x = new TextField("0");
        TextField y = new TextField("0");
        TextField z = new TextField("0");
        TextField scale = new TextField("1");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.addRow(0, new Label("X"), x);
        grid.addRow(1, new Label("Y"), y);
        grid.addRow(2, new Label("Z"), z);
        grid.addRow(3, new Label("Scale"), scale);

        Button apply = new Button("Apply to all objects");
        apply.setOnAction(event -> {
            try {
                engine.scene().setTransformOfAll(
                        Float.parseFloat(x.getText()),
                        Float.parseFloat(y.getText()),
                        Float.parseFloat(z.getText()),
                        Float.parseFloat(scale.getText()));
            } catch (NumberFormatException e) {
                showStatus("Position and scale must be numbers.");
            }
        });
        return section("Transform", grid, apply);
    }

    private Node cameraSection() {
        Label help = new Label("Click the view, then W A S D to move, Space / Shift to go up / down, "
                + "drag with the mouse to look around.");
        help.setWrapText(true);
        return section("Camera",
                slider("Move speed", 0.01, 5.0, engine.scene().cameraMoveSpeed(),
                        value -> engine.scene().setCameraMoveSpeed((float) value)),
                help);
    }

    private Node shaderSection(String initialShader) {
        List<String> names = engine.sceneShaders();
        ComboBox<String> shaders = new ComboBox<>(FXCollections.observableArrayList(names));
        // An unknown name falls back to the first (default) shader, just like the engine does.
        shaders.setValue(names.contains(initialShader) ? initialShader : names.get(0));
        shaders.valueProperty().addListener((obs, previous, name) -> engine.requestShader(name));
        return section("Shader", shaders);
    }

    private Node postProcessSection() {
        PostProcessSettings s = postProcess;
        return section("Post processing",
                effect("Tone mapping", s.toneMapping, on -> s.toneMapping = on,
                        setting("Exposure", 0.1, 5.0, s.exposure, v -> s.exposure = v)),
                effect("Bloom", s.bloom, on -> s.bloom = on,
                        setting("Strength", 0.0, 2.0, s.bloomStrength, v -> s.bloomStrength = v),
                        setting("Threshold", 0.0, 2.0, s.bloomThreshold, v -> s.bloomThreshold = v)),
                effect("Color grading", s.colorGrading, on -> s.colorGrading = on,
                        setting("Saturation", 0.0, 2.0, s.saturation, v -> s.saturation = v)),
                effect("Depth of field", s.depthOfField, on -> s.depthOfField = on,
                        setting("Focus", 0.0, 1.0, s.dofFocus, v -> s.dofFocus = v),
                        setting("Blur scale", 0.0, 10.0, s.dofScale, v -> s.dofScale = v)),
                effect("Motion blur", s.motionBlur, on -> s.motionBlur = on,
                        setting("Strength", 0.0, 1.0, s.motionBlurStrength, v -> s.motionBlurStrength = v)),
                effect("Vignette", s.vignette, on -> s.vignette = on,
                        setting("Intensity", 0.0, 1.0, s.vignetteIntensity, v -> s.vignetteIntensity = v)),
                effect("Film grain", s.filmGrain, on -> s.filmGrain = on,
                        setting("Intensity", 0.0, 0.2, s.filmGrainIntensity, v -> s.filmGrainIntensity = v)),
                effect("SSAO", s.ssao, on -> s.ssao = on,
                        setting("Strength", 0.0, 2.0, s.ssaoStrength, v -> s.ssaoStrength = v),
                        setting("Radius", 0.0, 0.1, s.ssaoRadius, v -> s.ssaoRadius = v)),
                effect("Contact shadows", s.contactShadows, on -> s.contactShadows = on,
                        setting("Strength", 0.0, 1.0, s.contactShadowStrength, v -> s.contactShadowStrength = v)));
    }

    // ---- Small building blocks --------------------------------------------------------------

    private Optional<Path> chooseModelFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open 3D model");
        chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        List<String> patterns = engine.supportedModelExtensions().stream().map(ext -> "*." + ext).toList();
        if (!patterns.isEmpty()) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("3D models", patterns));
        }
        File file = chooser.showOpenDialog(content.getScene().getWindow());
        return Optional.ofNullable(file).map(File::toPath);
    }

    /** Applies a change to the post-process settings and sends them to the engine. */
    private void editPostProcess(Runnable change) {
        change.run();
        engine.requestPostProcessSettings(postProcess);
    }

    /** A checkbox for an effect, with its parameters indented below (greyed out when off). */
    private Node effect(String name, boolean enabled, Consumer<Boolean> setEnabled, Node... parameters) {
        CheckBox toggle = new CheckBox(name);
        toggle.setSelected(enabled);
        toggle.selectedProperty().addListener((obs, was, on) -> editPostProcess(() -> setEnabled.accept(on)));
        VBox parameterBox = new VBox(4, parameters);
        parameterBox.setPadding(new Insets(0, 0, 0, 24));
        parameterBox.disableProperty().bind(toggle.selectedProperty().not());
        return new VBox(4, toggle, parameterBox);
    }

    /** A slider bound to one float field of the post-process settings. */
    private Node setting(String label, double min, double max, float initial, Consumer<Float> set) {
        return slider(label, min, max, initial, value -> editPostProcess(() -> set.accept((float) value)));
    }

    private static Node slider(String label, double min, double max, double initial, DoubleConsumer onChange) {
        Slider slider = new Slider(min, max, initial);
        Label caption = new Label();
        Runnable updateCaption = () -> caption.setText(String.format("%s: %.3f", label, slider.getValue()));
        updateCaption.run();
        slider.valueProperty().addListener((obs, previous, value) -> {
            updateCaption.run();
            onChange.accept(value.doubleValue());
        });
        return new VBox(2, caption, slider);
    }

    private static Node section(String title, Node... children) {
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        VBox box = new VBox(6, header);
        box.getChildren().addAll(children);
        return box;
    }
}
