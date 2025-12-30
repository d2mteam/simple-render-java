package com.simplerender.app;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.util.Optional;

public final class ModelFileDialog {
    private ModelFileDialog() {
    }

    public static Optional<Path> chooseModelFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select 3D Model");
        chooser.setFileFilter(new FileNameExtensionFilter("3D Models", "obj", "gltf"));
        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        return Optional.of(chooser.getSelectedFile().toPath());
    }
}
