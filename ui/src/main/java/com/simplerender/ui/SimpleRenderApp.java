package com.simplerender.ui;

import com.simplerender.engine.Engine;
import com.simplerender.engine.EngineHost;
import com.simplerender.scene.InputState;
import java.nio.ByteBuffer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * The JavaFX application: builds the window and hosts the {@link Engine}.
 *
 * <p>JavaFX owns the application lifecycle. {@link #start} creates the engine and starts
 * its render thread; {@link #stop} (window closed) shuts it down again. The two sides only
 * meet through {@link EngineHost} (engine to UI) and {@link Engine}'s request methods
 * (UI to engine).
 */
public final class SimpleRenderApp extends Application {
    private Engine engine;
    private ControlPanel controlPanel;

    @Override
    public void start(Stage stage) {
        LaunchOptions options = LaunchOptions.parse(getParameters().getRaw());
        engine = new Engine(options.engineConfig());

        FrameView frameView = new FrameView();
        InputCollector input = new InputCollector();
        input.attachTo(frameView.node());
        resizeEngineWith(frameView.node());
        controlPanel = new ControlPanel(engine, options.engineConfig().shaderName());

        BorderPane root = new BorderPane(frameView.node());
        root.setLeft(controlPanel.node());
        stage.setTitle("Simple Render");
        stage.setScene(new Scene(root, 1280, 720));
        stage.show();

        engine.start(new EngineHost() {
            @Override
            public InputState pollInput() {
                return input.poll();
            }

            @Override
            public void onFrame(ByteBuffer bgraPixels, int width, int height) {
                frameView.onFrame(bgraPixels, width, height);
            }

            @Override
            public void engineStopped(Throwable error) {
                Platform.runLater(() -> onEngineStopped(error));
            }
        });

        frameView.node().requestFocus();
        if (options.modelPath() != null) {
            controlPanel.loadModel(options.modelPath());
        } else {
            controlPanel.showStatus("No model loaded. Click \"Load model…\" or start with --model <file>.");
        }
    }

    @Override
    public void stop() {
        if (controlPanel != null) {
            controlPanel.shutdown();
        }
        if (engine != null) {
            engine.stop();
        }
    }

    /** Keeps the engine's render size equal to the view's size. */
    private void resizeEngineWith(Region view) {
        InvalidationListener resize = observable -> engine.requestResize(
                (int) Math.max(1, Math.round(view.getWidth())),
                (int) Math.max(1, Math.round(view.getHeight())));
        view.widthProperty().addListener(resize);
        view.heightProperty().addListener(resize);
    }

    /** The render loop ended on its own: either --max-frames was reached or it crashed. */
    private void onEngineStopped(Throwable error) {
        if (error != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, String.valueOf(error));
            alert.setHeaderText("The render engine stopped");
            alert.showAndWait();
        }
        Platform.exit();
    }
}
