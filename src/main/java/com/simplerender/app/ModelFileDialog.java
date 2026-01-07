package com.simplerender.app;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Swing-based file picker for selecting model files.
 */
public final class ModelFileDialog {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ModelFileDialog.class);

    private ModelFileDialog() {
    }

    public static Optional<Path> chooseModelFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select 3D Model");
        chooser.setFileFilter(new FileNameExtensionFilter("3D Models", "obj", "gltf", "glb"));
        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            logger.info("Model selection canceled");
            return Optional.empty();
        }
        Path selected = chooser.getSelectedFile().toPath();
        logger.info("Model selected: {}", selected);
        return Optional.of(selected);
    }
}
